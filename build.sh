#!/bin/bash
# Build script for Nonton App

set -e

echo "🎬 Building Nonton App..."

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check if gradlew exists
if [ ! -f "./gradlew" ]; then
    print_error "gradlew not found. Please run from project root."
    exit 1
fi

# Make gradlew executable
chmod +x ./gradlew

# Parse arguments
BUILD_TYPE="debug"
FLAVOR="phone"
TASK="assemble"

while [[ $# -gt 0 ]]; do
    case $1 in
        --release)
            BUILD_TYPE="release"
            shift
            ;;
        --phone)
            FLAVOR="phone"
            shift
            ;;
        --tablet)
            FLAVOR="tablet"
            shift
            ;;
        --tv)
            FLAVOR="tv"
            shift
            ;;
        --clean)
            TASK="clean assemble"
            shift
            ;;
        --test)
            TASK="test"
            shift
            ;;
        --lint)
            TASK="lint"
            shift
            ;;
        --help)
            echo "Usage: ./build.sh [options]"
            echo "Options:"
            echo "  --release     Build release variant"
            echo "  --phone       Build phone variant (default)"
            echo "  --tablet      Build tablet variant"
            echo "  --tv          Build TV variant"
            echo "  --clean       Clean before build"
            echo "  --test        Run tests"
            echo "  --lint        Run lint"
            echo "  --help        Show this help"
            exit 0
            ;;
        *)
            print_error "Unknown option: $1"
            exit 1
            ;;
    esac
done

# Determine task
if [ "$TASK" = "assemble" ]; then
    GRADLE_TASK="assemble${FLAVOR^}${BUILD_TYPE^}"
else
    GRADLE_TASK="${TASK}${FLAVOR^}${BUILD_TYPE^}"
fi

print_status "Building: $GRADLE_TASK"

# Run Gradle
./gradlew $GRADLE_TASK --no-daemon --parallel --configure-on-demand

if [ $? -eq 0 ]; then
    print_status "Build successful! 🎉"

    # Show APK location
    if [ "$TASK" = "assemble" ]; then
        APK_PATH="app/build/outputs/apk/${FLAVOR}/${BUILD_TYPE}/app-${FLAVOR}-${BUILD_TYPE}.apk"
        if [ -f "$APK_PATH" ]; then
            print_status "APK: $APK_PATH"
            ls -lh "$APK_PATH"
        fi
    fi
else
    print_error "Build failed!"
    exit 1
fi