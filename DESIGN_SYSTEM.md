# FitTrack Design System
## Premium Fitness Experience

---

## 🎯 Design Philosophy

**"Motion is Life"** — Every element should feel alive, responsive, and encouraging.

### Core Principles
1. **Celebration** — Make every achievement feel rewarding
2. **Clarity** — Complex data, simple presentation
3. **Momentum** — UI that encourages the next action
4. **Personal** — Adapts to user's goals and patterns

---

## 🎨 Visual Identity

### Color System

#### Dynamic Theme (Adapts to time of day)
```
Morning (5AM-12PM):   Energetic oranges, warm yellows
Afternoon (12PM-6PM): Vibrant greens, fresh teals  
Evening (6PM-10PM):   Calming purples, deep blues
Night (10PM-5AM):     Dark mode, subtle accents
```

#### Core Palette
```kotlin
// Primary - Energy Gradient
val EnergyStart = Color(0xFFFF6B6B)      // Coral
val EnergyEnd = Color(0xFFFFE66D)        // Golden

// Secondary - Achievement
val AchievementGold = Color(0xFFFFD700)
val AchievementBronze = Color(0xFFCD7F32)

// Macros - Distinct & Accessible
val ProteinElectric = Color(0xFF00D4FF)   // Cyan
val CarbsEnergy = Color(0xFFFFB800)       // Amber
val FatRich = Color(0xFFFF6B9D)           // Pink

// Semantic
val SuccessGlow = Color(0xFF00E676)
val WarningPulse = Color(0xFFFFAB00)
val ErrorVibrant = Color(0xFFFF5252)

// Surfaces (Dark Mode First)
val SurfaceDeep = Color(0xFF0D0D0F)
val SurfaceElevated = Color(0xFF1A1A1F)
val SurfaceCard = Color(0xFF242429)
val SurfaceGlass = Color(0x40FFFFFF)      // 25% white
```

### Typography
```kotlin
// Display - For big numbers and celebrations
val DisplayLarge = 57.sp, Weight.Black, LetterSpacing(-0.25)
val DisplayMedium = 45.sp, Weight.Bold

// Headlines - Section titles
val HeadlineLarge = 32.sp, Weight.Bold
val HeadlineMedium = 28.sp, Weight.SemiBold

// Body - Content
val BodyLarge = 16.sp, Weight.Normal, LineHeight(24.sp)
val BodyMedium = 14.sp, Weight.Normal

// Numbers - Stats (use tabular figures)
val NumberLarge = 48.sp, Weight.Black, FontFeature("tnum")
val NumberMedium = 32.sp, Weight.Bold, FontFeature("tnum")
```

### Spacing Scale
```kotlin
val SpaceXS = 4.dp
val SpaceSM = 8.dp
val SpaceMD = 16.dp
val SpaceLG = 24.dp
val SpaceXL = 32.dp
val Space2XL = 48.dp
val Space3XL = 64.dp
```

### Corner Radius
```kotlin
val RadiusSM = 8.dp    // Chips, small buttons
val RadiusMD = 16.dp   // Cards
val RadiusLG = 24.dp   // Modals, bottom sheets
val RadiusXL = 32.dp   // FABs, featured cards
val RadiusFull = 999.dp // Pills, avatars
```

---

## 🧩 Component Library

### 1. Activity Rings (Apple Fitness-inspired but evolved)
```
- 3D perspective with subtle rotation on scroll
- Particle effects on completion
- Haptic feedback at milestones (25%, 50%, 75%, 100%)
- Glow effect that intensifies with progress
- Animated gradient stroke
```

### 2. Macro Orbs (Innovative)
```
Instead of boring progress bars:
- Floating liquid orbs that fill up
- Physics-based wobble animation
- Merge animation when you hit goals
- Color shifts as you approach target
```

### 3. Streak Flame
```
- Animated fire icon that grows with streak length
- Different flame colors for streak milestones (7, 30, 100 days)
- Gentle pulse animation
- Particle embers floating up
```

### 4. Food Cards (Tinder-style Quick Log)
```
- Swipe right to log, left to skip
- Hold to customize portions
- Beautiful food photography backgrounds
- Macro preview on hover/long-press
```

### 5. Workout Cards (Premium Feel)
```
- Glassmorphism with blur background
- Progress indicator as card border
- Expand animation for exercise details
- Rest timer with breathing animation
```

### 6. Achievement Badges (3D Collectibles)
```
- 3D rendered badges with rotation
- Unlock animation with particles
- Rarity tiers (Common, Rare, Epic, Legendary)
- Showcase display in profile
```

---

## ✨ Micro-interactions

### Logging a Meal
```
1. Card appears with spring animation
2. Portions increment with satisfying "tick" haptic
3. Confirm: Card shrinks into calorie ring
4. Ring animates progress with particle trail
5. If goal met: Celebration burst
```

### Completing a Set
```
1. Check animation (Lottie)
2. Weight/rep numbers flip animation
3. Volume counter increments smoothly
4. PR detection: Golden glow + badge unlock
5. Rest timer starts with breathing guide
```

### Opening the App
```
1. Logo pulse animation
2. Today's stats fly in from edges
3. Greeting adapts to time + streak
4. Quick actions fade up with stagger
```

---

## 📱 Screen Designs

### Home Screen (Dashboard)
```
┌─────────────────────────────────────┐
│  Good morning, Sebastian 🔥 12 days │  <- Streak badge
├─────────────────────────────────────┤
│                                     │
│    ╭───────────────────────╮        │
│    │   🎯 CALORIE RING     │        │  <- 3D animated ring
│    │      1,847            │        │
│    │    ────────────       │        │
│    │    2,200 goal         │        │
│    ╰───────────────────────╯        │
│                                     │
│  ┌─────┐  ┌─────┐  ┌─────┐         │
│  │ 🔵  │  │ 🟡  │  │ 🩷  │         │  <- Macro orbs
│  │120g │  │180g │  │ 65g │         │
│  │PROT │  │CARB │  │ FAT │         │
│  └─────┘  └─────┘  └─────┘         │
│                                     │
├─────────────────────────────────────┤
│  ⚡ ACTIVITY          from Watch   │
│  ┌─────────────────────────────────┐│
│  │ 🚶 8,432 steps    🔥 2,340 cal ││  <- Glassmorphism card
│  │ ❤️ 72 avg bpm     😴 7h 23m    ││
│  └─────────────────────────────────┘│
├─────────────────────────────────────┤
│  🍽️ TODAY'S MEALS                  │
│                                     │
│  [Breakfast ✓] [Lunch ✓] [Dinner]  │  <- Pill selectors
│                                     │
│  ┌─────────────────────────────────┐│
│  │ 🥗 Chicken Salad       450 kcal ││
│  │ 🍳 Eggs & Toast        320 kcal ││
│  │ ➕ Quick Add                     ││
│  └─────────────────────────────────┘│
│                                     │
└─────────────────────────────────────┘
     [🏠]  [🍽️]  [➕]  [💪]  [👤]
              Quick Log FAB
```

### Quick Log FAB (Innovative)
```
Tap the center FAB:
- Expands into radial menu
- Options: 🍽️ Meal, 💧 Water, 💪 Workout, ⚖️ Weight
- Each has quick-log presets
- Voice input option: "Log 2 eggs and toast"
```

### Workout Screen (During Exercise)
```
┌─────────────────────────────────────┐
│  ← PUSH DAY                  45:32  │  <- Timer
├─────────────────────────────────────┤
│                                     │
│  ┌─────────────────────────────────┐│
│  │        BENCH PRESS              ││
│  │                                 ││
│  │   SET 3 of 4                    ││
│  │                                 ││
│  │      ┌─────────────────┐        ││
│  │      │      185        │        ││  <- Big, tappable
│  │      │       lb        │        ││
│  │      └────────┬────────┘        ││
│  │               │                 ││
│  │      ┌────────┴────────┐        ││
│  │      │       8         │        ││
│  │      │      reps       │        ││
│  │      └─────────────────┘        ││
│  │                                 ││
│  │  ← Previous: 185 × 8           ││
│  │                                 ││
│  │      [ ✓ LOG SET ]             ││  <- Primary action
│  │                                 ││
│  └─────────────────────────────────┘│
│                                     │
│  ┌──────────────────────────────── │
│  │ Set 1: 135×12  Set 2: 165×10   ││  <- Previous sets
│  └──────────────────────────────── │
│                                     │
│  NEXT: Incline Dumbbell Press →    │
│                                     │
└─────────────────────────────────────┘
```

---

## 🎮 Gamification System

### Levels & XP
```
Every action earns XP:
- Log meal: 10 XP
- Complete workout: 50 XP
- Hit calorie goal: 25 XP
- New PR: 100 XP
- 7-day streak: 200 XP

Levels unlock:
- Level 5: Custom themes
- Level 10: Advanced analytics
- Level 20: AI meal suggestions
- Level 50: "Elite" badge
```

### Achievements
```
🥇 First Steps - Log your first meal
🔥 On Fire - 7 day streak
💪 Century Club - 100 workouts logged
🎯 Bullseye - Hit macros perfectly 
🏆 PR Machine - Set 10 personal records
⭐ Perfectionist - 30 days of perfect logging
```

### Social Features (Optional)
```
- Weekly challenges with friends
- Leaderboards (opt-in)
- Share achievements as beautiful cards
- Workout buddy matching
```

---

## 🤖 AI-Powered Features

### Smart Suggestions
```
"Based on your meals today, you need 45g more protein.
Here are some options that fit your remaining calories:"
[🥚 3 Eggs - 21g] [🍗 Chicken Breast - 31g] [🥛 Protein Shake - 25g]
```

### Workout Intelligence
```
"You haven't trained legs in 5 days. 
Your squat has been progressing well.
Ready for Leg Day?"
[Start Leg Day] [Maybe Later]
```

### Pattern Recognition
```
"You tend to overeat on weekends. 
Want me to adjust your weekday calories
to give you more flexibility on Saturday?"
```

---

## 📐 Responsive Behavior

### Tablet/Foldable
```
- Two-column layout
- Persistent sidebar navigation
- Expanded charts and analytics
- Side-by-side meal + workout view
```

### Wear OS Companion
```
- Glanceable stats
- Quick log from wrist
- Workout controls
- Heart rate sync
```

---

## 🔊 Sound Design

```
- Subtle "pop" on successful log
- Achievement unlock fanfare
- Gentle chime on goal completion
- Workout timer beeps (optional)
- Rest complete notification
```

---

## 💫 Animation Specs

All animations use:
- Spring physics for natural feel
- 60fps minimum
- Reduced motion support
- Consistent easing: FastOutSlowIn

```kotlin
val DefaultSpring = spring(
    dampingRatio = 0.7f,
    stiffness = 300f
)

val BouncySpring = spring(
    dampingRatio = 0.5f,
    stiffness = 400f
)
```
