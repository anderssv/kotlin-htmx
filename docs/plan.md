# TDD Plan: Connected Browsers Counter

## Feature Description
Display the number of connected browsers to the checkbox demo page. The counter updates in real-time when clients connect or when dead connections are detected during broadcasts.

## Implementation Approach
- Add method to get connected client count
- Broadcast client count when new client registers
- Broadcast client count when dead connection is removed during existing broadcast operations
- Display "Connected browsers: N" at the top of the checkbox grid
- Use SSE to update the counter across all clients

---

## Tests (TDD Order)

### [x] Test 1: Get connected client count
**File**: `src/test/kotlin/no/mikill/kotlin_htmx/HtmxCheckboxDemoPageTest.kt`

**Test**: `HtmxCheckboxDemoPage` should expose a method to get the current number of connected clients.

**Implementation**: Add `getConnectedClientCount(): Int` method that returns `connectedListeners.size`

**Status**: ✅ COMPLETED

---

### [x] Test 2: Broadcast client count on registration
**File**: `src/test/kotlin/no/mikill/kotlin_htmx/HtmxCheckboxDemoPageTest.kt`

**Test**: When `registerOnCheckBoxNotification()` is called, it should broadcast the new client count to all connected clients.

**Implementation**:
- Add `broadcastClientCount()` method
- Call it from `registerOnCheckBoxNotification()` after adding the session
- Use SSE event name "update-client-count"

**Status**: ✅ COMPLETED

---

### [x] Test 3: Client count updates when dead connection detected
**File**: Unit test for `HtmxCheckboxDemoPage`

**Test**: When `broadcastUpdate()` detects a dead connection (IOException), it should broadcast updated client count after removing the dead connection.

**Implementation**:
- Call `broadcastClientCount()` after `iterator.remove()` in the existing catch block in `broadcastUpdate()`

**Status**: ✅ COMPLETED

---

### [x] Test 4: Render client counter in UI
**File**: `src/test/kotlin/no/mikill/kotlin_htmx/HtmxCheckboxPageTest.kt`

**Test**: The checkbox page should render a div with id "client-counter" containing the text "Connected browsers: ".

**Implementation**:
- Add UI element in `renderBoxGridHtml()` at the top, before existing counter div
- Include SSE swap attribute: `attributes["sse-swap"] = "update-client-count"`
- Initial count should be "0" or calculated value

**Status**: ✅ COMPLETED

---

### [x] Test 5: Client counter updates in browser (Selenium)
**File**: `src/test/kotlin/no/mikill/kotlin_htmx/HtmxCheckboxPageTest.kt`

**Test**:
- Open first browser, verify counter shows "Connected browsers: 1"
- Open second browser, verify both show "Connected browsers: 2"
- Close second browser, wait briefly
- Click a checkbox in first browser (triggers dead connection detection)
- Verify first browser shows "Connected browsers: 1"

**Implementation**: All previous tests passed. Client counter updates correctly in real-time across browsers.

**Status**: ✅ COMPLETED

---

## Notes
- Reuse existing dead connection detection in `broadcastUpdate()` - no need for separate polling
- Client count includes the current browser
- Dead connections are detected and removed during normal broadcast operations
