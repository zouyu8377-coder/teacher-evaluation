# Design System Document: The Scholastic Prism

## 1. Overview & Creative North Star
**Creative North Star: The Intellectual Atelier**

This design system moves beyond the "utilitarian grid" typical of enterprise software. For an educational evaluation platform, the interface must feel authoritative yet inspiring—combining the precision of an academic journal with the fluid, high-tech energy of a modern laboratory. 

We achieve this through **"The Layered Editorial"** approach: 
- **Intentional Asymmetry:** Overlapping data cards and staggered layouts break the monotonous "dashboard box" feel.
- **Tonal Depth:** Using Material-inspired surface tiers to create a hierarchy of information without the clutter of lines.
- **Vibrant Precision:** Using the high-intensity gradients as "data beacons" against a muted, sophisticated backdrop.

---

## 2. Colors: Tonal Orchestration
Our palette is divided into a "Quiet Foundation" and "Expressive Data."

### The Quiet Foundation (Surface Hierarchy)
To ensure the colorful gradients "pop," the foundation uses a sophisticated gray-blue scale.
- **Background (`#f7f9fc`):** The canvas. Always use this for the base page level.
- **Surface Tiers:** 
    - `surface-container-lowest` (`#ffffff`): For primary content cards.
    - `surface-container-low` (`#f2f4f7`): For secondary navigation or sidebar backgrounds.
    - `surface-container-high` (`#e6e8eb`): For subtle inset elements or hover states on buttons.

### The "No-Line" Rule
**Explicit Instruction:** Prohibit the use of 1px solid borders for sectioning. Boundaries must be defined solely through background color shifts. A `surface-container-lowest` card sitting on a `surface` background provides all the definition needed.

### The Gradient Spectrum (Data Beacons)
Use these gradients for high-impact metrics, progress indicators, and "Hero" cards:
- **Insight Purple:** `#8E2DE2` → `#4A00E0` (Primary Analytics)
- **Pulse Pink:** `#FF512F` → `#DD2476` (Urgent Alerts)
- **Growth Green:** `#11998e` → `#38ef7d` (Positive Trends)
- **Clarity Blue:** `#00c6ff` → `#0072ff` (User Metrics)

---

## 3. Typography: Editorial Authority
We utilize a dual-font strategy to balance character with readability.

### Display & Headlines (**Manrope**)
Manrope provides a geometric, modern precision.
- **Display-LG (3.5rem):** Reserved for "Impact Metrics" (e.g., total student enrollment numbers).
- **Headline-SM (1.5rem):** Used for section titles. Tracking should be set to -0.02em for a tighter, more premium feel.

### UI & Body (**Inter**)
Inter is the workhorse for data density and clarity.
- **Body-MD (0.875rem):** The standard for all dashboard labels and descriptions.
- **Label-SM (0.6875rem):** Used for micro-copy and data-viz legends. Use `on_surface_variant` (`#484456`) to maintain hierarchy.

---

## 4. Elevation & Depth: The Layering Principle
We reject traditional drop shadows in favor of **Tonal Layering** and **Ambient Diffusion**.

### Tonal Layering
Depth is achieved by "stacking." A card (`surface-container-lowest`) placed on a section (`surface-container-low`) creates a soft, natural lift.

### Ambient Shadows
When a card must "float" (e.g., on hover), use an extra-diffused shadow:
- **Value:** `0px 20px 40px rgba(25, 28, 30, 0.06)`
- **Color:** Always use a tinted version of `on_surface` at < 8% opacity. Never use pure black shadows.

### Glassmorphism & The "Ghost Border"
For floating modals or dropdowns, use **Glassmorphism**:
- `background`: `rgba(255, 255, 255, 0.7)`
- `backdrop-filter`: `blur(12px)`
- **Ghost Border:** A 1px border using `outline-variant` (`#c9c3d9`) at 20% opacity to catch the light.

---

## 5. Components: Enterprise Refinement

### Roundedness Scale
- **Cards/Buttons:** Use `xl` (`0.75rem`) for a friendly, modern enterprise feel.
- **Chips/Inputs:** Use `full` (`9999px`) for a "pill" aesthetic that contrasts against the rectangular grid.

### Cards (The "Prism" Card)
- **Resting State:** `surface-container-lowest`, no border, `xl` rounding.
- **Hover State:** Lift by 4px, apply **Ambient Shadow**, and add a 2px top-accent line using one of the signature gradients.

### Buttons
- **Primary:** Gradient background (Purple to Blue), `white` text, `xl` rounding. Use a subtle inner glow (1px top highlight) to simulate a premium tactile surface.
- **Tertiary:** No background, `primary` text. On hover, use `surface-container-high` background.

### Input Fields
- Avoid boxy borders. Use a "Soft-Underline" or a solid `surface-container-high` fill with a `primary` 2px bottom border that animates on focus.

### Data Visualization
- **High-Contrast Rule:** All charts must use the gradient palette. Ensure the `on_surface` color is used for axes and grid lines at 10% opacity to keep the focus on the data, not the chart apparatus.

---

## 6. Do's and Don'ts

### Do:
- **DO** use whitespace as a separator. If you think you need a divider line, increase the padding by 16px instead.
- **DO** overlap elements. Let a "Total Students" chip sit half-on and half-off a card header to create depth.
- **DO** use `surface-bright` for the most important interactive zones.

### Don't:
- **DON'T** use 100% opaque borders. They create "visual noise" that cheapens the enterprise feel.
- **DON'T** use the red gradient for anything other than critical errors or failing evaluations.
- **DON'T** use standard "system" fonts. Stick strictly to the Manrope/Inter pairing to maintain the editorial voice.