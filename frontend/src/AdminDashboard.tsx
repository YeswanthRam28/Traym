import React, { useState, useEffect } from 'react';

export default function AdminDashboard() {
  const [password, setPassword] = useState('');
  const [isAuth, setIsAuth] = useState(false);
  const [token, setToken] = useState('');
  const [users, setUsers] = useState<any[]>([]);
  const [aiPrompt, setAiPrompt] = useState('');
  const [isSaving, setIsSaving] = useState(false);
  const [error, setError] = useState('');
  const [stats, setStats] = useState({ totalUsers: 0, totalVolume: 0, maxVolume: 0 });
  const [isMaintenance, setIsMaintenance] = useState(false);
  
  // Messaging State
  const [selectedUsers, setSelectedUsers] = useState<Set<string>>(new Set());
  const [messageContent, setMessageContent] = useState('');
  const [targetType, setTargetType] = useState('broadcast');
  const [targetVersion, setTargetVersion] = useState('2');
  const [isSending, setIsSending] = useState(false);

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      const res = await fetch('/api/admin/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ password })
      });
      const data = await res.json();
      if (data.success) {
        setIsAuth(true);
        setToken(data.token);
        setError('');
      } else {
        setError('Invalid password');
      }
    } catch (err: any) {
      setError(err.message);
    }
  };

  useEffect(() => {
    if (isAuth) {
      fetchUsers();
      fetchPrompt();
      fetchStats();
      fetchMaintenance();
    }
  }, [isAuth]);

  const fetchStats = async () => {
    try {
      const res = await fetch('/api/admin/stats', {
        headers: { 'Authorization': `Bearer ${token}` }
      });
      const data = await res.json();
      setStats(data);
    } catch (err) {
      console.error(err);
    }
  };

  const fetchMaintenance = async () => {
    try {
      const res = await fetch('/api/admin/maintenance', {
        headers: { 'Authorization': `Bearer ${token}` }
      });
      const data = await res.json();
      setIsMaintenance(data.isMaintenance);
    } catch (err) {
      console.error(err);
    }
  };

  const toggleMaintenance = async () => {
    const newValue = !isMaintenance;
    if (!window.confirm(`Are you sure you want to turn ${newValue ? 'ON' : 'OFF'} Maintenance Mode? This will ${newValue ? 'block' : 'allow'} user access to the app.`)) return;
    setIsMaintenance(newValue);
    try {
      await fetch('/api/admin/maintenance', {
        method: 'POST',
        headers: { 
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}` 
        },
        body: JSON.stringify({ isMaintenance: newValue })
      });
    } catch (err) {
      console.error(err);
      setIsMaintenance(!newValue); // Revert on failure
    }
  };

  const fetchUsers = async () => {
    try {
      const res = await fetch('/api/admin/users', {
        headers: { 'Authorization': `Bearer ${token}` }
      });
      const data = await res.json();
      if (data.users) setUsers(data.users);
    } catch (err) {
      console.error(err);
    }
  };

  const fetchPrompt = async () => {
    try {
      const res = await fetch('/api/admin/prompts/ai_system_prompt', {
        headers: { 'Authorization': `Bearer ${token}` }
      });
      const data = await res.json();
      if (data.prompt) setAiPrompt(data.prompt);
    } catch (err) {
      console.error(err);
    }
  };

  const handleBanUser = async (id: string, isBanned: boolean) => {
    const action = isBanned ? 'unban' : 'ban';
    if (!window.confirm(`Are you sure you want to ${action} this user?`)) return;
    try {
      await fetch(`/api/admin/users/${id}/${action}`, {
        method: 'POST',
        headers: { 'Authorization': `Bearer ${token}` }
      });
      fetchUsers();
    } catch (err) {
      console.error(err);
    }
  };

  const handleSavePrompt = async () => {
    setIsSaving(true);
    try {
      await fetch('/api/admin/prompts', {
        method: 'POST',
        headers: { 
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}` 
        },
        body: JSON.stringify({ key: 'ai_system_prompt', content: aiPrompt })
      });
      alert('Prompt saved successfully!');
    } catch (err) {
      console.error(err);
      alert('Failed to save prompt.');
    } finally {
      setIsSaving(false);
    }
  };

  const toggleUserSelection = (id: string) => {
    const newSet = new Set(selectedUsers);
    if (newSet.has(id)) newSet.delete(id);
    else newSet.add(id);
    setSelectedUsers(newSet);
  };

  const handleSendMessage = async () => {
    if (!messageContent.trim()) {
      setError('Message cannot be empty.');
      return;
    }
    if (targetType === 'dms' && selectedUsers.size === 0) {
      setError('Please select at least one user from the leaderboard.');
      return;
    }
    
    setIsSending(true);
    setError('');
    
    try {
      await fetch('/api/admin/messages', {
        method: 'POST',
        headers: { 
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}` 
        },
        body: JSON.stringify({
          message: messageContent,
          targetType,
          targetUserIds: Array.from(selectedUsers),
          targetVersion: parseInt(targetVersion)
        })
      });
      setMessageContent('');
      setSelectedUsers(new Set());
      alert('Message sent successfully!');
    } catch (err) {
      setError('Failed to send message.');
      console.error(err);
    } finally {
      setIsSending(false);
    }
  };

  if (!isAuth) {
    return (
      <div className="min-h-screen bg-[#080808] text-[#F2F2F2] flex items-center justify-center p-4">
        <form onSubmit={handleLogin} className="bg-[#111] p-8 border border-[#2A2A2A] w-full max-w-md flex flex-col gap-6">
          <h1 className="font-syne text-2xl font-bold uppercase tracking-widest text-[#D6FF00]">Admin Access</h1>
          {error && <p className="text-red-500 text-sm">{error}</p>}
          <div>
            <label className="block text-xs text-[#666] tracking-widest uppercase mb-2">Command Password</label>
            <input 
              type="password" 
              value={password}
              onChange={e => setPassword(e.target.value)}
              className="w-full bg-[#1A1A1A] border border-[#333] text-white p-3 font-dm text-sm focus:outline-none focus:border-[#D6FF00]"
              placeholder="Enter password"
            />
          </div>
          <button type="submit" className="bg-[#D6FF00] text-black font-bold uppercase tracking-widest p-3 text-sm hover:bg-white transition-colors">
            Authenticate
          </button>
        </form>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-[#080808] text-[#F2F2F2] p-8 font-dm">
      <div className="max-w-6xl mx-auto space-y-12">
        <header className="flex justify-between items-end border-b border-[#2A2A2A] pb-6">
          <div>
            <h1 className="font-syne text-4xl font-extrabold uppercase tracking-tighter text-[#D6FF00]">Command Center</h1>
            <p className="text-[#666] text-sm mt-2 tracking-widest uppercase">God-Mode Enabled</p>
          </div>
          <div className="flex items-center gap-6">
            <div className="flex items-center gap-3 bg-[#111] border border-[#2A2A2A] px-4 py-2">
              <span className="text-xs uppercase tracking-widest text-white">App Lock</span>
              <button 
                onClick={toggleMaintenance}
                className={`w-12 h-6 rounded-full flex items-center p-1 transition-colors ${isMaintenance ? 'bg-red-500' : 'bg-green-500'}`}
              >
                <div className={`w-4 h-4 bg-white rounded-full shadow-md transform transition-transform ${isMaintenance ? 'translate-x-6' : 'translate-x-0'}`} />
              </button>
            </div>
            <button onClick={() => setIsAuth(false)} className="text-xs text-red-500 hover:text-red-400 uppercase tracking-widest">
              Logout
            </button>
          </div>
        </header>

        {/* Global Analytics */}
        <section className="grid grid-cols-1 sm:grid-cols-3 gap-6">
          <div className="bg-[#111] border border-[#2A2A2A] p-6 text-center">
            <h3 className="text-xs text-[#666] uppercase tracking-widest mb-2">Registered Athletes</h3>
            <p className="font-syne text-4xl font-bold text-white">{stats.totalUsers}</p>
          </div>
          <div className="bg-[#111] border border-[#2A2A2A] p-6 text-center">
            <h3 className="text-xs text-[#666] uppercase tracking-widest mb-2">Global Volume Lifted</h3>
            <p className="font-syne text-4xl font-bold text-[#D6FF00]">{stats.totalVolume.toLocaleString()} <span className="text-sm text-[#666]">kg</span></p>
          </div>
          <div className="bg-[#111] border border-[#2A2A2A] p-6 text-center">
            <h3 className="text-xs text-[#666] uppercase tracking-widest mb-2">Highest Single PR</h3>
            <p className="font-syne text-4xl font-bold text-white">{stats.maxVolume.toLocaleString()} <span className="text-sm text-[#666]">kg</span></p>
          </div>
        </section>

        {/* AI Prompt Management */}
        <section className="bg-[#111] border border-[#2A2A2A] p-6">
          <h2 className="font-syne text-xl uppercase tracking-widest text-white mb-4">AI Coach System Prompt</h2>
          <p className="text-[#666] text-sm mb-4">This prompt is fetched dynamically by the Android app. Changes here update the AI's personality and instructions instantly.</p>
          <textarea 
            value={aiPrompt}
            onChange={(e) => setAiPrompt(e.target.value)}
            className="w-full h-64 bg-[#1A1A1A] border border-[#333] text-[#F2F2F2] p-4 text-sm font-mono focus:outline-none focus:border-[#D6FF00] mb-4"
            placeholder="You are Traym, a premium, hyper-personalized AI strength and conditioning coach..."
          />
          <button 
            onClick={handleSavePrompt} 
            disabled={isSaving}
            className="bg-[#D6FF00] text-black font-bold uppercase tracking-widest px-6 py-2 text-sm hover:bg-white transition-colors disabled:opacity-50"
          >
            {isSaving ? 'Saving...' : 'Save Prompt'}
          </button>
        </section>

        {/* Leaderboard Management */}
        <section className="bg-[#111] border border-[#2A2A2A] p-6">
          <div className="flex justify-between items-center mb-6">
            <h2 className="font-syne text-xl uppercase tracking-widest text-white">Leaderboard Moderation</h2>
            <button onClick={fetchUsers} className="text-xs text-[#D6FF00] tracking-widest uppercase hover:text-white">Refresh Data</button>
          </div>
          
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead className="text-xs text-[#666] uppercase tracking-widest border-b border-[#2A2A2A]">
                <tr>
                  <th className="pb-3 font-normal">Rank</th>
                  <th className="pb-3 font-normal">Athlete Name</th>
                  <th className="pb-3 font-normal">XP (Volume)</th>
                  <th className="pb-3 font-normal text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-[#2A2A2A]">
                {users.map((user, idx) => (
                  <tr key={user.id} className={`hover:bg-[#1A1A1A] transition-colors ${user.is_banned ? 'opacity-50' : ''}`}>
                    <td className="py-4">
                      <input 
                        type="checkbox" 
                        checked={selectedUsers.has(user.id)}
                        onChange={() => toggleUserSelection(user.id)}
                        className="w-4 h-4 text-[#D6FF00] bg-transparent border-[#2A2A2A] rounded focus:ring-0 cursor-pointer"
                      />
                    </td>
                    <td className="py-4 text-[#D6FF00] font-bold">#{idx + 1}</td>
                    <td className="py-4 text-white flex items-center gap-2">
                      {user.name} 
                      {user.is_banned && <span className="text-red-500 text-xs uppercase">(Banned)</span>}
                      {user.is_notion_connected && (
                        <span className="bg-white text-black text-[10px] px-1.5 py-0.5 font-bold rounded uppercase tracking-widest">Notion</span>
                      )}
                    </td>
                    <td className="py-4 text-[#aaa]">{user.xp} kg</td>
                    <td className="py-4 text-right">
                      <button 
                        onClick={() => handleBanUser(user.id, user.is_banned)}
                        className={`${user.is_banned ? 'text-green-500 hover:text-green-400' : 'text-red-500 hover:text-red-400'} text-xs uppercase tracking-widest`}
                      >
                        {user.is_banned ? 'Unban' : 'Ban User'}
                      </button>
                    </td>
                  </tr>
                ))}
                {users.length === 0 && (
                  <tr>
                    <td colSpan={5} className="py-8 text-center text-[#666]">No users found in the database.</td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </section>

        {/* Inbox / Messaging System */}
        <section className="bg-[#111] border border-[#2A2A2A] p-6">
          <h2 className="font-syne text-xl uppercase tracking-widest text-white mb-4">Broadcast & DMs</h2>
          <div className="flex flex-col md:flex-row gap-6 mb-4">
            <div className="flex-1 space-y-4">
              <textarea
                value={messageContent}
                onChange={(e) => setMessageContent(e.target.value)}
                placeholder="Type your message here..."
                className="w-full h-32 bg-transparent border border-[#2A2A2A] text-white p-4 font-mono text-sm focus:outline-none focus:border-[#D6FF00] resize-none"
              />
            </div>
            <div className="w-full md:w-1/3 space-y-4">
              <div className="flex flex-col gap-2">
                <label className="text-xs uppercase tracking-widest text-[#666]">Target Audience</label>
                <select 
                  value={targetType} 
                  onChange={(e) => setTargetType(e.target.value)}
                  className="bg-transparent border border-[#2A2A2A] text-white p-3 font-mono text-sm focus:outline-none focus:border-[#D6FF00]"
                >
                  <option value="broadcast">Global Broadcast (All Users)</option>
                  <option value="dms">Selected Athletes ({selectedUsers.size} selected)</option>
                  <option value="outdated">Outdated Apps (Version Nag)</option>
                </select>
              </div>
              
              {targetType === 'outdated' && (
                <div className="flex flex-col gap-2">
                  <label className="text-xs uppercase tracking-widest text-[#666]">Target Version Code</label>
                  <input 
                    type="number"
                    value={targetVersion}
                    onChange={(e) => setTargetVersion(e.target.value)}
                    placeholder="e.g. 2"
                    className="bg-transparent border border-[#2A2A2A] text-white p-3 font-mono text-sm focus:outline-none focus:border-[#D6FF00]"
                  />
                  <p className="text-[10px] text-[#666]">Users with App Version LESS THAN this number will see the message.</p>
                </div>
              )}

              <button
                onClick={handleSendMessage}
                disabled={isSending}
                className="w-full bg-[#D6FF00] text-black font-syne font-bold uppercase tracking-widest py-3 hover:bg-white transition-colors disabled:opacity-50"
              >
                {isSending ? 'Sending...' : 'Send Message'}
              </button>
            </div>
          </div>
        </section>
      </div>
    </div>
  );
}
