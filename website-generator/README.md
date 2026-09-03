# Tree Website Generator 🌳

A standalone Java tool that transforms Android TreeApp backups (SQLite database + photos) into an interactive, read-only static website with auto-centered Leaflet maps, photo galleries, and species fruiting calendars — ready for **100% free hosting on GitHub Pages**.

---

## Features

- **🗺️ Interactive Satellite & Street Maps**: Powered by Leaflet.js with species-colored pins and popup cards.
- **🎯 Auto-Centered Collections**: Each collection map dynamically calculates its bounding box and centers on the trees in that collection.
- **📷 Photo Galleries with Captions**: Full photo history with dates and observation captions.
- **🌾 Scion & Grafting Records**: Track grafted varieties, rootstocks, and sources.
- **📅 Species & Fruiting Calendar**: Month-by-month harvest guide for all recorded botanical species.
- **🚀 100% Free Hosting**: No backend server needed — deploys automatically to GitHub Pages.

---

## Directory Structure

```
website-generator/
├── build.gradle                  # Build script (SQLite JDBC + Thymeleaf)
├── data/                         # Place your exported ZIP or .db here
│   └── TreeApp_Website_Export.zip
├── src/main/java/com/tree/
│   ├── SiteGenerator.java        # Main generator application
│   ├── beans/                    # Tree, Species, Collection, Location, Image, Note, Scion
│   ├── db/                       # WebTreeDAO (SQLite JDBC)
│   └── util/                     # ZipUtils
├── src/main/resources/
│   ├── static/css/style.css      # Modern responsive theme
│   └── templates/                # Thymeleaf HTML Templates
│       ├── index.html            # Dashboard & overview map
│       ├── collection.html       # Auto-centered collection map + table
│       ├── tree_detail.html      # Individual tree page + gallery + mini map
│       └── species.html          # Botanical directory & fruiting guide
└── dist/                         # Generated static website output
```

---

## Quick Start (Local Run)

1. Export your data from the Android phone app:
   - In the app, go to **Backup / Restore**.
   - Tap any backup and select **"Export for Website (DB + Photos ZIP)"**.
2. Copy the exported `TreeApp_Website_Export.zip` into `website-generator/data/`.
3. In a terminal, run:
   ```bash
   cd website-generator
   gradle run
   ```
4. Open `website-generator/dist/index.html` in your web browser!

---

## GitHub Pages Deployment

1. Push your repository to GitHub.
2. In your GitHub repository settings:
   - Go to **Settings** &rarr; **Pages**.
   - Under **Build and deployment**, set **Source** to **GitHub Actions**.
3. Whenever you upload or commit a new `TreeApp_Website_Export.zip` into the `data/` folder, GitHub Actions will automatically rebuild and publish your website within ~30 seconds.
