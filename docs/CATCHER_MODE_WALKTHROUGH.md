# Walkthrough: Universal Remote Remapper & DIGI Isolation

## What was Accomplished

### 1. Device-Specific Isolation (DIGI Q Workaround)
The hardcoded "DIGI Online" `Q` key workaround was specifically designed for ZTE R2A (DIGI) TV boxes. To prevent confusion for users on standard Android TV boxes:
- Added a device detection check: `Build.BRAND == "DIGI"` or `Build.MODEL` containing `"DIGI"` or `"R2A"`.
- If the app is running on a standard box, the `DIGI Online key action` setting is completely hidden from the user interface.
- The background `KeyInterceptorService` will only intercept `KEYCODE_Q` if the device is identified as a DIGI box.

### 2. Universal "Catcher Mode" (Custom Remote Buttons)
Users can now remap dedicated, useless hardware buttons (e.g., Netflix, Prime Video) on their remotes to launch custom apps, intents, or system actions.

#### The "Catcher" Logic
- Users click **"Add Button"** in the new "Custom Remote Buttons" UI section.
- The app enters **Catcher Mode**, showing a dialog and setting a background flag.
- `KeyInterceptorService` catches the *very next* keypress from the remote, strictly ignoring safe system navigation keys (D-pad arrows, OK, Back, Home, Volume) to prevent users from accidentally "bricking" their remote's primary navigation.
- The intercepted key is blocked from the OS, Catcher Mode is immediately disabled, and the `keyCode` is sent to the UI via `BroadcastReceiver`.

#### Action Assignment
- A dialog appears allowing the user to select an action for the newly caught button (e.g., Launch YouTube, System Back, Custom Intent).
- The mapping is saved as a JSON dictionary in `SharedPreferences`.

#### Execution
- In normal mode, if no physical keyboard is connected, `KeyInterceptorService` checks every remote keypress against the custom JSON dictionary.
- If a match is found, the original action is suppressed (`return true`), and the chosen custom action is executed cleanly using the universal `executeAction()` engine.

## Validation Results
- Code successfully compiled with no errors.
- UI renders correctly, hiding the Q-action block on non-DIGI devices.
- `SharedPreferences` logic correctly loads and applies the `custom_remaps` JSON.
