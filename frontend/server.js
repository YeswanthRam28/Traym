import express from 'express';
import cors from 'cors';
import pg from 'pg';
import dotenv from 'dotenv';
import path from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

// Load environment variables from the root project directory
dotenv.config({ path: path.join(__dirname, '../.env') });

const app = express();
app.use(cors());
app.use(express.json());

const PORT = process.env.PORT || 3001;
const ADMIN_PASSWORD = process.env.TRAYM_ADMIN_PASS || 'traym_admin_123';

// Neon DB Connection
const { Pool } = pg;

// Helper to remove channel_binding for pg compatibility
let dbUrl = process.env.NEON_DATABASE_URL || '';
if (dbUrl.includes('channel_binding=require')) {
    dbUrl = dbUrl.replace('&channel_binding=require', '').replace('channel_binding=require', '');
}

const pool = new Pool({
  connectionString: dbUrl,
  ssl: {
    rejectUnauthorized: false
  }
});

// Middleware for Admin Auth
const adminAuth = (req, res, next) => {
    const authHeader = req.headers['authorization'];
    if (!authHeader || authHeader !== `Bearer ${ADMIN_PASSWORD}`) {
        return res.status(401).json({ error: 'Unauthorized. Invalid admin password.' });
    }
    next();
};

// Initialize Admin Tables
async function initDb() {
    try {
        await pool.query(`
            CREATE TABLE IF NOT EXISTS system_prompts (
                key VARCHAR(255) PRIMARY KEY,
                content TEXT NOT NULL,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        `);
        // Add columns to leaderboard_users if they don't exist
        await pool.query(`
            ALTER TABLE leaderboard_users 
            ADD COLUMN IF NOT EXISTS is_banned BOOLEAN DEFAULT FALSE,
            ADD COLUMN IF NOT EXISTS latest_workout_json TEXT,
            ADD COLUMN IF NOT EXISTS is_notion_connected BOOLEAN DEFAULT FALSE
        `);

        // Create user_messages table for Direct Messaging and Announcements
        await pool.query(`
            CREATE TABLE IF NOT EXISTS user_messages (
                id SERIAL PRIMARY KEY,
                target_user_id VARCHAR(255),
                target_version INT,
                message TEXT NOT NULL,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        `);

        // Create Trigger to enforce banned users stay at -1 XP
        await pool.query(`
            CREATE OR REPLACE FUNCTION prevent_banned_xp_update()
            RETURNS TRIGGER AS $$
            BEGIN
                IF NEW.is_banned = TRUE THEN
                    NEW.xp = -1;
                    NEW.name = '[Banned]';
                END IF;
                RETURN NEW;
            END;
            $$ LANGUAGE plpgsql;
        `);
        
        await pool.query(`
            DROP TRIGGER IF EXISTS enforce_banned_xp ON leaderboard_users;
            CREATE TRIGGER enforce_banned_xp
            BEFORE INSERT OR UPDATE ON leaderboard_users
            FOR EACH ROW
            EXECUTE FUNCTION prevent_banned_xp_update();
        `);

        console.log('Database tables and triggers initialized successfully.');
    } catch (err) {
        console.error('Error initializing database tables:', err);
    }
}
initDb();

// --- API ENDPOINTS ---

// Check Auth
app.post('/api/admin/login', (req, res) => {
    const { password } = req.body;
    if (password === ADMIN_PASSWORD) {
        res.json({ success: true, token: ADMIN_PASSWORD });
    } else {
        res.status(401).json({ error: 'Invalid password' });
    }
});

// Get Users (Leaderboard)
app.get('/api/admin/users', adminAuth, async (req, res) => {
    try {
        const result = await pool.query('SELECT * FROM leaderboard_users ORDER BY xp DESC');
        res.json({ users: result.rows });
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

// Ban User
app.post('/api/admin/users/:id/ban', adminAuth, async (req, res) => {
    try {
        const { id } = req.params;
        // Updating the row will automatically fire the trigger and set xp = -1
        await pool.query('UPDATE leaderboard_users SET is_banned = TRUE WHERE id = $1', [id]);
        res.json({ success: true });
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

// Unban User
app.post('/api/admin/users/:id/unban', adminAuth, async (req, res) => {
    try {
        const { id } = req.params;
        await pool.query('UPDATE leaderboard_users SET is_banned = FALSE WHERE id = $1', [id]);
        res.json({ success: true });
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

// --- MESSAGING ENDPOINTS ---

// Send Message
app.post('/api/admin/messages', adminAuth, async (req, res) => {
    try {
        const { message, targetType, targetUserIds, targetVersion } = req.body;
        
        if (!message) return res.status(400).json({ error: 'Message is required' });
        
        if (targetType === 'broadcast' || targetType === 'outdated') {
            const version = targetType === 'outdated' ? parseInt(targetVersion) : null;
            await pool.query(
                'INSERT INTO user_messages (target_user_id, target_version, message) VALUES ($1, $2, $3)',
                [null, version, message]
            );
        } else if (targetType === 'dms' && Array.isArray(targetUserIds)) {
            for (const userId of targetUserIds) {
                await pool.query(
                    'INSERT INTO user_messages (target_user_id, target_version, message) VALUES ($1, $2, $3)',
                    [userId, null, message]
                );
            }
        }
        
        res.json({ success: true });
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

// Get AI Prompt
app.get('/api/admin/prompts/:key', adminAuth, async (req, res) => {
    try {
        const { key } = req.params;
        const result = await pool.query('SELECT * FROM system_prompts WHERE key = $1', [key]);
        if (result.rows.length > 0) {
            res.json({ prompt: result.rows[0].content });
        } else {
            res.json({ prompt: '' });
        }
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

// Save AI Prompt
app.post('/api/admin/prompts', adminAuth, async (req, res) => {
    try {
        const { key, content } = req.body;
        await pool.query(`
            INSERT INTO system_prompts (key, content) 
            VALUES ($1, $2) 
            ON CONFLICT (key) 
            DO UPDATE SET content = EXCLUDED.content, updated_at = CURRENT_TIMESTAMP
        `, [key, content]);
        res.json({ success: true });
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

// Android App Public API (Read-only) to fetch AI Prompt without password
app.get('/api/public/prompts/:key', async (req, res) => {
    try {
        const { key } = req.params;
        const result = await pool.query('SELECT * FROM system_prompts WHERE key = $1', [key]);
        if (result.rows.length > 0) {
            res.json({ prompt: result.rows[0].content });
        } else {
            res.status(404).json({ error: 'Prompt not found' });
        }
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

// --- ANALYTICS & MAINTENANCE ENDPOINTS ---

// Get Community Stats
app.get('/api/admin/stats', adminAuth, async (req, res) => {
    try {
        const usersRes = await pool.query('SELECT COUNT(*) as total FROM leaderboard_users WHERE is_banned = FALSE OR is_banned IS NULL');
        const volumeRes = await pool.query('SELECT SUM(xp) as total_volume FROM leaderboard_users WHERE xp > 0 AND (is_banned = FALSE OR is_banned IS NULL)');
        const maxRes = await pool.query('SELECT MAX(xp) as max_xp FROM leaderboard_users WHERE xp > 0 AND (is_banned = FALSE OR is_banned IS NULL)');
        
        res.json({
            totalUsers: parseInt(usersRes.rows[0].total) || 0,
            totalVolume: parseInt(volumeRes.rows[0].total_volume) || 0,
            maxVolume: parseInt(maxRes.rows[0].max_xp) || 0
        });
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

// Get Maintenance Status
app.get('/api/admin/maintenance', adminAuth, async (req, res) => {
    try {
        const result = await pool.query("SELECT content FROM system_prompts WHERE key = 'maintenance_mode'");
        const isMaintenance = result.rows.length > 0 ? result.rows[0].content === 'true' : false;
        res.json({ isMaintenance });
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

// Toggle Maintenance Status
app.post('/api/admin/maintenance', adminAuth, async (req, res) => {
    try {
        const { isMaintenance } = req.body;
        await pool.query(`
            INSERT INTO system_prompts (key, content) 
            VALUES ('maintenance_mode', $1) 
            ON CONFLICT (key) 
            DO UPDATE SET content = EXCLUDED.content, updated_at = CURRENT_TIMESTAMP
        `, [isMaintenance ? 'true' : 'false']);
        res.json({ success: true });
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});
// Health Check Endpoint (Public)
app.get('/api/health', (req, res) => {
    res.json({ status: 'ok', timestamp: new Date().toISOString() });
});

// Serve static files from the React frontend build
const distPath = path.join(__dirname, 'dist');
app.use(express.static(distPath));

app.get('*', (req, res) => {
    res.sendFile(path.join(distPath, 'index.html'));
});

app.listen(PORT, () => {
  console.log(`Admin Backend Server running on port ${PORT}`);
  
  // Render Keep-Alive Bot
  // Uses RENDER_EXTERNAL_URL in production, or localhost in development
  const renderUrl = process.env.RENDER_EXTERNAL_URL || `http://localhost:${PORT}`;
  const pingIntervalMinutes = 3;
  
  console.log(`[Keep-Alive] Bot activated. Pinging ${renderUrl}/api/health every ${pingIntervalMinutes} minutes.`);
  
  setInterval(() => {
      console.log(`[Keep-Alive] Firing health check ping to ${renderUrl}...`);
      fetch(`${renderUrl}/api/health`)
          .then(res => res.json())
          .then(data => console.log(`[Keep-Alive] Success! Server timestamp: ${data.timestamp}`))
          .catch(err => console.error(`[Keep-Alive] Error:`, err.message));
          
      console.log(`[Keep-Alive] Next ping scheduled in ${pingIntervalMinutes} minutes.`);
  }, pingIntervalMinutes * 60 * 1000); // 3 minutes
});
