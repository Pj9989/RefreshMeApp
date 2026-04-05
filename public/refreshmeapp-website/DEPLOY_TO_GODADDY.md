# How to Deploy RefreshMe Website to GoDaddy (refreshmeapp.com)

## What's Included in This Website Package

| File | Purpose |
|------|---------|
| `index.html` | Main homepage — hero, features, how it works, pricing, download |
| `style.css` | All styling — dark theme, gold/purple branding matching the app |
| `script.js` | Navbar scroll, mobile menu, animations |
| `privacy.html` | Privacy Policy page |
| `terms.html` | Terms of Service page |
| `assets/logo.webp` | App icon (square) |
| `assets/logo_round.webp` | App icon (round) — used in navbar and favicon |

---

## Option A: Upload via GoDaddy File Manager (Easiest)

1. Log in to **account.godaddy.com**
2. Click on **Refresh Me** → **Website** → **Manage**
3. In the website builder, look for **"Edit Website"** or **"File Manager"**
4. If using GoDaddy's **cPanel/Hosting**:
   - Go to **cPanel → File Manager**
   - Navigate to the `public_html` folder
   - Delete any existing files (backup first if needed)
   - Upload all files from this package: `index.html`, `style.css`, `script.js`, `privacy.html`, `terms.html`
   - Create an `assets/` folder and upload `logo.webp` and `logo_round.webp` into it

---

## Option B: Use GoDaddy FTP (Recommended for Full Control)

1. In GoDaddy, go to **Hosting → Manage → FTP Users**
2. Note your FTP hostname, username, and password
3. Use an FTP client like [FileZilla](https://filezilla-project.org/) (free)
4. Connect using your FTP credentials
5. Navigate to `public_html/`
6. Upload all files maintaining the folder structure:
   ```
   public_html/
   ├── index.html
   ├── style.css
   ├── script.js
   ├── privacy.html
   ├── terms.html
   └── assets/
       ├── logo.webp
       └── logo_round.webp
   ```

---

## Option C: Replace GoDaddy Website Builder Site

If your GoDaddy site is currently using the **GoDaddy Website Builder** (Airo):

1. Log in to GoDaddy → Click **Refresh Me** → **Website**
2. Go to **Settings** → look for **"Switch to WordPress"** or **"Use my own files"**
3. Alternatively, point your domain to a **hosting plan** where you can upload custom HTML
4. GoDaddy's free plan may require upgrading to use custom HTML — check **View Plans**

---

## Option D: Deploy to Firebase Hosting (Already Connected)

Since your app already uses Firebase, this is the cleanest option:

```bash
# In your terminal, navigate to the website folder
cd refreshmeapp-website

# Install Firebase CLI if not already installed
npm install -g firebase-tools

# Login to Firebase
firebase login

# Initialize hosting (select your existing project)
firebase init hosting
# When asked "What do you want to use as your public directory?" → type: . (dot)
# Configure as single-page app? → No
# Set up automatic builds? → No

# Deploy
firebase deploy --only hosting
```

Then in Firebase Console → Hosting → Add Custom Domain → enter `refreshmeapp.com`

---

## After Deployment — Update These Links

Once live, update the following in `index.html`:

1. **App Store link** (line with `Download on iOS`) — replace `href="#"` with your actual App Store URL
2. **Google Play link** — replace with your actual Play Store URL  
3. **Social media links** — update Instagram, Twitter, Facebook URLs in the footer
4. **Contact email** — currently set to `support@refreshmeapp.com`

---

## Domain Setup (if needed)

If `refreshmeapp.com` isn't pointing to your hosting yet:

1. GoDaddy → **Domain** → **Manage DNS**
2. Add an **A record**: `@` → your hosting IP address
3. Add a **CNAME**: `www` → `refreshmeapp.com`
4. DNS changes take up to 24 hours to propagate

---

*Website built to match RefreshMe Android app branding: dark theme (#0D0D0D), gold (#D4AF37), purple (#BB86FC)*
