import http.client
import json
import os
import time

def fetch_all_exercises():
    print("Fetching exercises from ExerciseDB with pagination...")
    conn = http.client.HTTPSConnection("exercisedb.p.rapidapi.com")

    headers = {
        'x-rapidapi-key': "39d4c7d724msh48bea839fcbd664p17d019jsn67f5e1cf7dee",
        'x-rapidapi-host': "exercisedb.p.rapidapi.com",
        'Content-Type': "application/json"
    }

    all_exercises = []
    limit = 10
    offset = 0

    while offset < 1400: # API has ~1300 exercises
        conn.request("GET", f"/exercises?limit={limit}&offset={offset}", headers=headers)
        res = conn.getresponse()
        
        if res.status == 429:
            print("Rate limited. Sleeping for 2 seconds...")
            time.sleep(2)
            continue
            
        if res.status != 200:
            print(f"Failed to fetch data: {res.status} {res.reason}")
            break

        data = res.read()
        try:
            exercises = json.loads(data.decode("utf-8"))
        except json.JSONDecodeError:
            print("Failed to decode JSON")
            break
            
        if not exercises:
            break
            
        all_exercises.extend(exercises)
        print(f"Fetched {len(exercises)} items (Offset: {offset}). Total so far: {len(all_exercises)}")
        
        if len(exercises) < limit:
            break
            
        offset += limit
        time.sleep(0.5)
    
    print(f"Successfully fetched {len(all_exercises)} total exercises.")
    
    assets_dir = r"D:\Projects\Traym\app\src\main\assets"
    if not os.path.exists(assets_dir):
        os.makedirs(assets_dir)
        
    output_path = os.path.join(assets_dir, "exercises_db.json")
    with open(output_path, "w", encoding="utf-8") as f:
        json.dump(all_exercises, f, indent=4)
        
    print(f"Saved to {output_path}")

if __name__ == "__main__":
    fetch_all_exercises()
