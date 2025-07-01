# AGENT.md - GTL ACS Project

## Build/Lint/Test Commands
- `npm run dev` - Start development server
- `npm run build` - Build for production
- `npm run lint` - Run ESLint
- `npm run format` - Format code with Prettier
- `npm test` - No tests configured (returns exit 0)
- `quasar dev` - Alternative dev command
- `quasar build` - Alternative build command

## Architecture & Structure
- **Frontend**: Vue 3 + Quasar Framework + TypeScript
- **State Management**: Pinia stores in `src/stores/`
- **Routing**: Vue Router with hash mode, routes in `src/router/routes.ts`
- **Internationalization**: Vue I18n with files in `src/i18n/`
- **Main Directories**: `components/`, `pages/`, `layouts/`, `services/`, `utils/`, `workers/`
- **ACS Application**: Antenna Control System with dashboard modes (standby, step, slew, etc.)

## Code Style & Conventions
- **Imports**: Use type imports (`import type`) for types only
- **Formatting**: Prettier with single quotes, no semicolons, 100 char width
- **Linting**: ESLint with Vue/TypeScript configs, strict TypeScript enabled
- **Components**: Use Vue 3 Composition API, components in `src/components/`
- **Naming**: Korean comments allowed, English for code/functions
- **File Extensions**: `.vue` for components, `.ts` for TypeScript files
