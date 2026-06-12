import fs from 'fs';

const SOURCE_URL = 'https://raw.githubusercontent.com/yuhonas/free-exercise-db/main/dist/exercises.json';

async function main() {
  console.log('Downloading exercises from free-exercise-db...');
  
  const response = await fetch(SOURCE_URL);
  if (!response.ok) {
    console.error('Failed to fetch:', response.statusText);
    process.exit(1);
  }
  
  const raw = await response.json();
  console.log(`Downloaded ${raw.length} exercises.`);

  const transformed = raw.map(item => ({
    id: item.id,
    name: item.name,
    bodyPart: item.primaryMuscles?.[0] ?? 'full body',
    equipment: item.equipment ?? 'body only',
    target: item.primaryMuscles?.[0] ?? 'full body',
    secondaryMuscles: item.secondaryMuscles ?? [],
    instructions: item.instructions ?? [],
    description: (item.instructions ?? []).join(' '),
    difficulty: item.level ?? 'beginner',
    category: item.category ?? 'strength'
  }));

  const targetPath = '../app/src/main/assets/exercises_db.json';
  fs.writeFileSync(targetPath, JSON.stringify(transformed, null, 2));
  console.log(`Done! Wrote ${transformed.length} exercises to ${targetPath}`);
}

main();
