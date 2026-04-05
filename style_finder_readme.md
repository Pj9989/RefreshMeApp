# AI Style Finder Feature: README

This document outlines the new files created for the AI Style Finder feature and provides instructions on how to set up and test it.

## Summary of New/Modified Files

### Android App (`/app`)

*   **Navigation:**
    *   `app/src/main/res/navigation/nav_graph_customer.xml`: Modified to include the new quiz and results screens and the navigation actions between them.

*   **Home Screen:**
    *   `app/src/main/res/layout/fragment_home.xml`: Modified to add the "AI Style Finder" card as an entry point.
    *   `app/src/main/java/com/refreshme/home/HomeFragment.kt`: Modified to handle clicks on the new card and navigate to the quiz.
    *   `app/src/main/res/drawable/ic_style_finder.xml`: New icon for the feature card.

*   **Data Models:**
    *   `app/src/main/java/com/refreshme/data/stylefinder/Style.kt`: Data model for a hairstyle in the `/styles` collection.
    *   `app/src/main/java/com/refreshme/data/stylefinder/AiStyleRequest.kt`: Data model for a request document in `/aiStyleRequests`.
    *   `app/src/main/java/com/refreshme/data/stylefinder/AiStyleResult.kt`: Data model for the JSON result from the AI.

*   **Quiz Screen:**
    *   `app/src/main/res/layout/fragment_ai_style_quiz.xml`: Layout for the quiz screen.
    *   `app/src/main/java/com/refreshme/aistylefinder/AiStyleQuizFragment.kt`: UI controller for the quiz screen.
    *   `app/src/main/java/com/refreshme/aistylefinder/AiStyleQuizViewModel.kt`: ViewModel for the quiz screen.

*   **Results Screen:**
    *   `app/src/main/res/layout/fragment_ai_style_results.xml`: Layout for the results screen.
    *   `app/src/main/java/com/refreshme/aistylefinder/AiStyleResultsFragment.kt`: UI controller for the results screen.
    *   `app/src/main/java/com/refreshme/aistylefinder/AiStyleResultsViewModel.kt`: ViewModel for the results screen.
    *   `app/src/main/res/layout/item_recommended_style.xml`: Layout for a single recommended style item.
    *   `app/src/main/java/com/refreshme/aistylefinder/RecommendedStyleAdapter.kt`: RecyclerView adapter for the list of recommended styles.

### Cloud Functions (`/functions`)

*   `functions/src/index.ts`: Modified to add the `generateStyleRecommendations` Cloud Function.
*   `functions/package.json`: You will need to add a new dependency for the Google AI SDK.

## Setup Instructions

### 1. Firebase/Cloud Setup

*   **Enable Vertex AI API:** In your Google Cloud project, ensure the "Vertex AI API" is enabled.
*   **Set Cloud Function Secret:** The Cloud Function requires an API key for the Gemini model. Set this as a secret in Firebase:
    ```bash
    firebase functions:secrets:set GEMINI_API_KEY
    ```
    When prompted, paste your API key. This key is securely stored and accessible by your function.

### 2. Update Cloud Function Dependencies

Navigate to the `functions` directory and add the Vertex AI client library:

```bash
cd functions
npm install @google-cloud/vertexai
```

### 3. Deploy the Cloud Function

Deploy the new function to Firebase:

```bash
firebase deploy --only functions
```

### 4. Populate Firestore Data

For the feature to work, you need to populate the `/styles` collection in Firestore. Create a few documents with the following structure:

*   **Collection:** `styles`
*   **Document ID:** (auto-id)
*   **Fields:**
    *   `name`: (string) e.g., "Classic Taper Fade"
    *   `tags`: (array of strings) e.g., ["fade", "classic", "short"]
    *   `worksForFaceShapes`: (array of strings) e.g., ["oval", "square"]
    *   `worksForHairTypes`: (array of strings) e.g., ["straight", "wavy"]
    *   `maintenance`: (string) e.g., "low"
    *   `barberScript`: (string) e.g., "A classic taper fade, finger-length on top."

## How to Test

1.  **Launch the App:** Run the app on an emulator or device and log in as a customer.
2.  **Navigate to Home:** Go to the main home screen. You should see the new "AI Style Finder" card at the bottom.
3.  **Start the Quiz:** Tap the card to open the quiz screen.
4.  **Answer Questions:** Fill out the quiz form.
5.  **Submit:** Tap the "Find My Style" button.
6.  **View Results:** You will be navigated to the results screen.
    *   Initially, you will see a "processing" or "queued" status.
    *   After a few seconds, the Cloud Function should complete, and the screen will update to show 3-5 recommended hairstyles.
    *   Below the hairstyles, a list of recommended stylists (if any match the style tags) will be displayed.
7.  **Check Firestore:** You can monitor the process in the Firebase console. A new document will be created in the `aiStyleRequests` collection, and its `status` field will transition from `queued` -> `processing` -> `done` (or `error`).

This completes the implementation of the AI Style Finder feature. Let me know if you have any questions!