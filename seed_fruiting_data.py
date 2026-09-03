import sqlite3
import re
import os

DB_PATH = 'phone_db.sqlite'
REPORTS = [
    r'c:\treeApp\Totnes_Fruiting_Report.txt',
    r'c:\treeApp\France_Fruiting_Report.txt'
]

MONTH_MAP = {
    'january': 1, 'february': 2, 'march': 3, 'april': 4, 'may': 5, 'june': 6,
    'july': 7, 'august': 8, 'september': 9, 'october': 10, 'november': 11, 'december': 12,
    'jan': 1, 'feb': 2, 'mar': 3, 'apr': 4, 'may': 5, 'jun': 6,
    'jul': 7, 'aug': 8, 'sep': 9, 'oct': 10, 'nov': 11, 'dec': 12
}

def get_start_month(text):
    text = text.lower()
    # Find all months mentioned
    found_months = []
    for month_name, month_num in MONTH_MAP.items():
        if month_name in text:
            # Check for word boundary to avoid partial matches
            if re.search(r'\b' + month_name + r'\b', text):
                # Find the position
                pos = text.find(month_name)
                found_months.append((pos, month_num))
    
    if not found_months:
        return None
    
    # Sort by position in string and take the first one
    found_months.sort()
    return found_months[0][1]

def clean_description(desc):
    if not desc:
        return desc
    # Remove noisy parentheses containing variety lists or generic context
    desc = re.sub(r'\(Apple,.*?\)', '', desc)
    desc = re.sub(r'\(Pear,.*?\)', '', desc)
    desc = re.sub(r'\(.*?\bis [A-Z].*?\)', '', desc) # removes ( 'Var' is Oct) etc
    desc = desc.replace('  ', ' ').strip()
    return desc

def get_variety_key(variety):
    if not variety:
        return ""
    v = variety.lower().replace("'", "").replace("-", " ").strip()
    # Handle common variants
    if "kids" in v and "grange" in v:
        return "kidds grange red"
    return v

def update_db():
    if not os.path.exists(DB_PATH):
        print(f"Error: Database not found at {DB_PATH}")
        return

    conn = sqlite3.connect(DB_PATH)
    cursor = conn.cursor()

    # Ensure schema is updated (Migration 48)
    print("Checking/Updating schema...")
    for table, col in [("TreeSpecies", "fruitingStartMonth"), ("TreeSpecies", "fruitingDescription"), 
                       ("Scion", "fruitingStartMonth"), ("Scion", "fruitingDescription")]:
        try:
            cursor.execute(f"ALTER TABLE {table} ADD COLUMN {col} {'INTEGER' if 'Month' in col else 'TEXT'}")
        except sqlite3.OperationalError:
            pass

    # First pass: Collect all data
    species_defaults = {} # latinName -> (start_month, desc)
    variety_data = {} # (species, normalized_variety) -> (start_month, desc)
    
    for report_path in REPORTS:
        if not os.path.exists(report_path):
            print(f"Warning: Report not found at {report_path}")
            continue
        
        print(f"Parsing {report_path}...")
        with open(report_path, 'r', encoding='utf-8', errors='ignore') as f:
            content = f.read()
        
        blocks = re.split(r'\nSpecies: ', content)
        for block in blocks:
            if not block.strip() or '===' in block:
                continue
            
            lines = block.strip().split('\n')
            if lines[0].startswith('Tree Fruiting'):
                continue
                
            species = lines[0].strip()
            variety = ""
            fruiting_desc = ""
            
            for line in lines[1:]:
                if line.startswith('Variety: '):
                    variety = line.replace('Variety: ', '').strip()
                elif line.startswith('Expected Fruiting: '):
                    fruiting_desc = line.replace('Expected Fruiting: ', '').strip()
            
            if not species or not fruiting_desc:
                continue
            
            start_month = get_start_month(fruiting_desc)
            clean_desc = clean_description(fruiting_desc)
            
            if variety.lower() in ['unknown', 'variety', '?', '0']:
                # Only overwrite if new one is shorter/cleaner or we don't have one
                if species not in species_defaults or len(clean_desc) < len(species_defaults[species][1]):
                    species_defaults[species] = (start_month, clean_desc)
            else:
                # Handle comma separated varieties in report
                for v in variety.split(','):
                    v_key = get_variety_key(v)
                    variety_data[(species, v_key)] = (start_month, clean_desc)

    # Second pass: Update Database
    print("Updating Database...")
    
    # 1. Update TreeSpecies and apply defaults to ALL scions
    for species, (month, desc) in species_defaults.items():
        print(f"Applying Species Default: {species} -> {desc}")
        cursor.execute("UPDATE TreeSpecies SET fruitingStartMonth = ?, fruitingDescription = ? WHERE latinName = ?", 
                       (month, desc, species))
        cursor.execute("UPDATE Scion SET fruitingStartMonth = ?, fruitingDescription = ? WHERE species = ?", 
                       (month, desc, species))

    # 2. Update specific variety data
    # Before we do this, let's get all scions from DB to do matching
    cursor.execute("SELECT scionId, species, variety FROM Scion")
    all_scions = cursor.fetchall()
    
    for scion_id, species, variety in all_scions:
        v_key = get_variety_key(variety)
        if (species, v_key) in variety_data:
            month, desc = variety_data[(species, v_key)]
            print(f"Applying Variety Specifics to ID {scion_id}: {species} {variety} -> {desc}")
            cursor.execute("UPDATE Scion SET fruitingStartMonth = ?, fruitingDescription = ? WHERE scionId = ?", 
                           (month, desc, scion_id))

    conn.commit()
    conn.close()
    print("Done!")

if __name__ == "__main__":
    update_db()
