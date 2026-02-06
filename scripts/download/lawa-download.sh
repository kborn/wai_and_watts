#!/bin/bash

# LAWA Water Quality Data Download Script
# 
# Downloads LAWA water quality workbooks from LAWA website
# Saves to ./downloads/lawa/YYYY-MM-DD/ directory
#
# Usage:
#   ./scripts/download/lawa-download.sh [URL]
#   ./scripts/download/lawa-download.sh  # Uses default URL
#
# Requirements:
#   - curl (for downloading files)
#   - bash (for script execution)
#

set -euo pipefail

# Configuration
DEFAULT_URL="https://www.lawa.org.nz/media/ftmb4fjn/lawa-river-water-quality-state-and-trend-results_30oct2025.xlsx"
DOWNLOAD_BASE_DIR="./downloads/lawa"
TIMESTAMP=$(date +%Y-%m-%d)
TARGET_DIR="${DOWNLOAD_BASE_DIR}/${TIMESTAMP}"

# Function to print usage
print_usage() {
    echo "LAWA Water Quality Data Download Script"
    echo ""
    echo "Downloads LAWA water quality workbooks from LAWA website"
    echo ""
    echo "Usage:"
    echo "  $0 [OPTIONS] [URL]"
    echo "  $0 --help"
    echo ""
    echo "Arguments:"
    echo "  URL    Optional: Direct URL to LAWA Excel workbook"
    echo "         Defaults to: ${DEFAULT_URL}"
    echo ""
    echo "Options:"
    echo "  --output-dir DIR    Specify output directory (default: ./downloads/lawa/)"
    echo "  --help             Show this help message"
    echo ""
    echo "Examples:"
    echo "  $0"
    echo "  $0 --output-dir /tmp/downloads https://www.lawa.org.nz/media/ftmb4fjn/lawa-river-water-quality-state-and-trend-results_30oct2025.xlsx"
    echo ""
    echo "The script will:"
    echo "  1. Create YYYY-MM-DD directory in output location"
    echo "  2. Download the Excel workbook"
    echo "  3. Validate the download"
    echo "  4. Report file path and size"
    echo ""
    echo "IMPORTANT: The ingestion system expects contract CSV files."
    echo "Use ./scripts/transform.sh to convert the downloaded Excel workbook to the"
    echo "canonical contract CSV before running ingestion."
}

# Function to create target directory
create_target_dir() {
    mkdir -p "${TARGET_DIR}"
    echo "Created target directory: ${TARGET_DIR}"
}

# Function to download file
download_file() {
    local url="$1"
    local filename=$(basename "${url}")
    local target_file="${TARGET_DIR}/${filename}"
    
    echo "Downloading: ${url}"
    echo "Target file: ${target_file}"
    
    # Download with curl showing progress
    curl -L --fail --show-error --progress-bar -o "${target_file}" "${url}"
    
    # Verify file exists and has content
    if [ ! -f "${target_file}" ] || [ ! -s "${target_file}" ]; then
        echo "ERROR: Download failed or file is empty"
        exit 1
    fi
    
    local file_size=$(du -h "${target_file}" | cut -f1)
    echo "Download completed successfully"
    echo "File size: ${file_size}"
    echo "File path: ${target_file}"
}

# Function to validate curl is available
validate_dependencies() {
    if ! command -v curl &> /dev/null; then
        echo "ERROR: curl is required but not installed"
        echo "Please install curl and try again"
        exit 1
    fi
}

# Function to validate URL format
validate_url() {
    local url="$1"
    
    # Basic URL validation - check if it starts with http:// or https://
    if [[ ! "${url}" =~ ^https?:// ]]; then
        echo "ERROR: Invalid URL format: ${url}"
        echo "URL must start with http:// or https://"
        exit 1
    fi
}

# Parse command line arguments
URL="${DEFAULT_URL}"

if [ "$#" -eq 0 ]; then
    # No arguments provided, use default URL
    echo "No arguments provided, using default LAWA workbook URL"
elif [ "$#" -eq 1 ]; then
    if [ "$1" == "--help" ] || [ "$1" == "-h" ]; then
        print_usage
        exit 0
    else
        URL="$1"
    fi
elif [ "$#" -eq 2 ]; then
    if [ "$1" == "--output-dir" ]; then
        DOWNLOAD_BASE_DIR="$2"
        echo "Using custom output directory: ${DOWNLOAD_BASE_DIR}"
    else
        echo "ERROR: Invalid arguments"
        echo "Use --help for usage information"
        exit 1
    fi
elif [ "$#" -eq 3 ]; then
    if [ "$1" == "--output-dir" ]; then
        DOWNLOAD_BASE_DIR="$2"
        if [ "$3" == "--help" ] || [ "$3" == "-h" ]; then
            print_usage
            exit 0
        else
            URL="$3"
        fi
    else
        echo "ERROR: Invalid arguments"
        echo "Use --help for usage information"
        exit 1
    fi
else
    echo "ERROR: Too many arguments provided"
    echo "Use --help for usage information"
    exit 1
fi

# Update target directory based on final DOWNLOAD_BASE_DIR
TIMESTAMP=$(date +%Y-%m-%d)
TARGET_DIR="${DOWNLOAD_BASE_DIR}/${TIMESTAMP}"

# Main execution
echo "=== LAWA Water Quality Data Download ==="
echo "Timestamp: ${TIMESTAMP}"
echo "Download directory: ${TARGET_DIR}"
echo ""

# Validate dependencies and URL
validate_dependencies
validate_url "${URL}"

# Create target directory
create_target_dir

# Download the file
download_file "${URL}"

echo ""
echo "=== Download Summary ==="
echo "Timestamp: ${TIMESTAMP}"
echo "Source URL: ${URL}"
echo "Target directory: ${TARGET_DIR}"
echo ""
echo "Next steps:"
echo "1. Transform to contract CSV:"
echo "   ./scripts/transform.sh lawa.water_quality.state.multi_year \"${TARGET_DIR}/$(basename "${URL}")\" /tmp/lawa_state_multi_year.csv"
echo "   ./scripts/transform.sh lawa.water_quality.trend.multi_year \"${TARGET_DIR}/$(basename "${URL}")\" /tmp/lawa_trend_multi_year.csv"
echo "2. Ingest the contract CSV:"
echo "   ./scripts/ingest.sh lawa.water_quality.state.multi_year /tmp/lawa_state_multi_year.csv $(date +%Y-%m-%d) \"LAWA Downloaded Workbook\""
echo "   ./scripts/ingest.sh lawa.water_quality.trend.multi_year /tmp/lawa_trend_multi_year.csv $(date +%Y-%m-%d) \"LAWA Downloaded Workbook\""
