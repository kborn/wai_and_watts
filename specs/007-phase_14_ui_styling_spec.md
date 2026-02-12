# Phase 14 --- UI / UX Styling Specification

## Design Direction

Clean, modern, data-product aesthetic. Not flashy. Feels trustworthy and
professional.

------------------------------------------------------------------------

## Design Tokens

### Spacing Scale

4, 8, 12, 16, 24, 32, 48

### Typography

-   H1: Page titles
-   H2: Section headers
-   Body: Primary text
-   Caption: Table metadata / timestamps

### Colors

Neutral background Primary accent for actions Muted border colors
Distinct success / error / refusal colors

------------------------------------------------------------------------

## Core Components

### Button

Primary + Secondary + Ghost variants

### Card

Used for: - Explanation results - Refusal display - Error states

### Table

Default data presentation Charts supplement tables, never replace them

### Callout

Used for: - Refusal - Error - Warnings

------------------------------------------------------------------------

## Page Anatomy

### Ask Page

Header Question Input Submit Button Example Questions Result Navigation

### Results Page

Question Echo Explanation Card Citation Section

### Data Pages (MBIE / LAWA)

Filter Controls Chart (Top) Table (Primary Data) Explain This CTA

------------------------------------------------------------------------

## Charts

Charts must: - Reflect selected filters - Use existing backend data
only - Avoid client-side metric invention

------------------------------------------------------------------------

## Responsive Expectations

Desktop first Tablet acceptable Mobile usable but not perfect

------------------------------------------------------------------------

## Non-Goals

No heavy dashboard system No visualization library lock-in yet No
client-side data science logic
