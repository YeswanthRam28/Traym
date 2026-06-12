import fs from 'fs';
import crypto from 'crypto';

const API_KEY = 'i1Fa6ot1Ve5hOlUwzrviWIn0swRbPWHbXZe7mHrL';
const URL = 'https://api.api-ninjas.com/v1/exercises';

const muscles = [
  'abdominals', 'abductors', 'adductors', 'biceps', 'calves',
  'chest', 'forearms', 'glutes', 'hamstrings', 'lats',
  'lower_back', 'middle_back', 'neck', 'quadriceps', 'traps', 'triceps'
];

const difficulties = ['beginner', 'intermediate', 'expert'];

const types = [
  'cardio', 'olympic_weightlifting', 'plyometrics',
  'powerlifting', 'strength', 'stretching', 'strongman'
];

const DELAY_MS = 150; // stay under rate limits

async function fetchPage(params) {
  const qs = new URLSearchParams(params);
  const response = await fetch(`${URL}?${qs}`, {
    headers: { 'X-Api-Key': API_KEY }
  });

  if (!response.ok) {
    console.error(`  HTTP ${response.status} — ${response.statusText}`);
    return [];
  }

  return response.json();
}

function normalizeExercise(item) {
  return {
    id: crypto.randomUUID(),
    name: item.name,
    bodyPart: item.muscle,
    // API returns equipment as a plain string, not an array
    equipment: item.equipment || 'bodyweight',
    target: item.muscle,
    secondaryMuscles: [],
    instructions: [item.instructions],
    description: item.instructions,
    difficulty: item.difficulty,
    category: item.type
  };
}

async function main() {
  const allExercisesMap = new Map();

  console.log('Starting full exercise download (muscle × difficulty × type)...\n');

  for (const muscle of muscles) {
    for (const difficulty of difficulties) {
      for (const type of types) {
        const label = `muscle=${muscle}, difficulty=${difficulty}, type=${type}`;
        
        try {
          // Free tier doesn't support offset, so we just get the first 10 for each permutation
          const items = await fetchPage({ muscle, difficulty, type });

          let added = 0;
          for (const item of items) {
            if (!allExercisesMap.has(item.name)) {
              allExercisesMap.set(item.name, normalizeExercise(item));
              added++;
            }
          }
          if (items.length > 0) {
              console.log(`Fetching ${label} … ${items.length} fetched, ${added} new`);
          }
        } catch (err) {
          console.error(`\n  Request failed: ${err.message}`);
        }

        await new Promise(r => setTimeout(r, DELAY_MS));
      }
    }
  }

  const allExercises = Array.from(allExercisesMap.values());
  console.log(`\nTotal unique exercises: ${allExercises.length}`);

  const targetPath = '../app/src/main/assets/exercises_db.json';
  fs.writeFileSync(targetPath, JSON.stringify(allExercises, null, 2));
  console.log(`Written to ${targetPath}`);
}

main();
