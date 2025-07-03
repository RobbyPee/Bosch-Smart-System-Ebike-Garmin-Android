#!/usr/bin/env python3
"""
nRF Connect Hex Message Extractor

This script extracts hex messages from nRF Connect log files.
It looks for lines containing: "(0x) [hex-data]" received
and outputs just the hex data to a text file.

Usage:
    python hex_extractor.py input_file.txt [output_file.txt]
    
Example:
    python hex_extractor.py paste.txt extracted_hex.txt
"""

import re
import sys
import os

def extract_hex_messages(input_file, output_file):
    """
    Extract hex messages from nRF Connect log file.
    
    Args:
        input_file: Path to the input log file
        output_file: Path to the output text file for hex messages
    
    Returns:
        int: Number of hex messages extracted
    """
    
    # Pattern to match lines with hex messages
    # Looks for: "(0x) [hex-data]" received
    pattern = r'"?\(0x\)\s+([A-F0-9\-]+)"\s+received'
    
    hex_messages = []
    
    try:
        with open(input_file, 'r', encoding='utf-8') as file:
            for line_num, line in enumerate(file, 1):
                match = re.search(pattern, line, re.IGNORECASE)
                if match:
                    hex_data = match.group(1)
                    hex_messages.append(hex_data)
                    if len(hex_messages) <= 10:  # Only show first 10 to avoid spam
                        print(f"Line {line_num}: Found hex message")
                    elif len(hex_messages) == 11:
                        print("... (showing first 10, continuing extraction)")
        
        # Write extracted hex messages to output file
        with open(output_file, 'w', encoding='utf-8') as file:
            for hex_msg in hex_messages:
                file.write(hex_msg + '\n')
        
        print(f"\nExtraction complete!")
        print(f"Found {len(hex_messages)} hex messages")
        print(f"Output saved to: {output_file}")
        
        # Show first few examples
        if hex_messages:
            print(f"\nFirst 3 extracted messages:")
            for i, msg in enumerate(hex_messages[:3]):
                print(f"{i+1}: {msg}")
        
        return len(hex_messages)
    
    except FileNotFoundError:
        print(f"Error: Could not find file '{input_file}'")
        return 0
    except Exception as e:
        print(f"Error processing file: {e}")
        return 0

def main():
    """Main function to handle command line arguments."""
    if len(sys.argv) < 2:
        print("Usage: python hex_extractor.py input_file.txt [output_file.txt]")
        print("Example: python hex_extractor.py paste.txt extracted_hex.txt")
        sys.exit(1)
    
    input_file = sys.argv[1]
    
    # Generate output filename if not provided
    if len(sys.argv) >= 3:
        output_file = sys.argv[2]
    else:
        base_name = os.path.splitext(input_file)[0]
        output_file = f"{base_name}_hex.txt"
    
    # Check if input file exists
    if not os.path.exists(input_file):
        print(f"Error: Input file '{input_file}' does not exist.")
        sys.exit(1)
    
    print(f"Processing: {input_file}")
    print(f"Output will be saved to: {output_file}")
    print("-" * 50)
    
    count = extract_hex_messages(input_file, output_file)
    
    if count > 0:
        print(f"\n✓ Successfully extracted {count} hex messages!")
    else:
        print("\n✗ No hex messages found or extraction failed.")

if __name__ == "__main__":
    main()