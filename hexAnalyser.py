import re
from collections import defaultdict, Counter

# Optional imports for enhanced features
try:
    import matplotlib.pyplot as plt
    HAS_MATPLOTLIB = True
except ImportError:
    HAS_MATPLOTLIB = False
    print("Note: matplotlib not available, plotting disabled")

try:
    import pandas as pd
    HAS_PANDAS = True
except ImportError:
    HAS_PANDAS = False
    print("Note: pandas not available, CSV export will use basic format")

class BLEMessageAnalyzer:
    def __init__(self):
        self.messages = []
        self.data_by_id = defaultdict(list)
        self.data_types = Counter()
        self.id_stats = defaultdict(lambda: {'count': 0, 'min': None, 'max': None, 'values': []})
        
    def parse_hex_data(self, hex_string):
        """Parse hex string and extract individual messages"""
        # Remove spaces and convert to uppercase
        hex_clean = hex_string.replace(' ', '').replace('-', '').upper()
        
        messages = []
        i = 0
        
        while i < len(hex_clean):
            # Look for message start (30)
            if i + 1 < len(hex_clean) and hex_clean[i:i+2] == '30':
                # Get length
                if i + 3 < len(hex_clean):
                    length_hex = hex_clean[i+2:i+4]
                    try:
                        length = int(length_hex, 16)
                        # Extract full message: 30 + length byte + payload
                        msg_end = i + 4 + (length * 2)  # *2 because each byte is 2 hex chars
                        if msg_end <= len(hex_clean):
                            message = hex_clean[i:msg_end]
                            messages.append(message)
                            i = msg_end
                        else:
                            i += 2
                    except ValueError:
                        i += 2
                else:
                    i += 2
            else:
                i += 2
                
        return messages
    
    def parse_message(self, message):
        """Parse individual 30-XX message - special handling for specific fields"""
        if len(message) < 6:  # Minimum: 30 + length + some data
            return None
            
        # Convert to byte array
        bytes_data = [message[i:i+2] for i in range(0, len(message), 2)]
        
        if bytes_data[0] != '30':
            return None
            
        length = int(bytes_data[1], 16)
        
        if length == 2:
            # Format: 30-02-[ID]-[VALUE]
            if len(bytes_data) >= 4:
                data_id = bytes_data[2]
                value = int(bytes_data[3], 16)  # Only last byte is value
                return {
                    'type': '30-02',
                    'data_id': data_id,
                    'data_type': None,
                    'value': value,
                    'raw': message
                }
                
        elif length >= 4:
            # Check for special field 30-07-98-2D-08-...
            if (length == 7 and len(bytes_data) >= 8 and 
                bytes_data[2] == '98' and bytes_data[3] == '2D' and bytes_data[4] == '08'):
                
                # Special handling: 30-07-98-2D-08-AA-BB-CC-DD
                # Treat AA-BB-CC-DD as 32-bit data
                value_bytes = bytes_data[5:9]  # AA, BB, CC, DD (4 bytes)
                
                if len(value_bytes) == 4:
                    # Try both little-endian and big-endian interpretations
                    # Little endian: DD-CC-BB-AA
                    value_le = (int(value_bytes[0], 16) +
                               (int(value_bytes[1], 16) << 8) +
                               (int(value_bytes[2], 16) << 16) +
                               (int(value_bytes[3], 16) << 24))
                    
                    # Big endian: AA-BB-CC-DD
                    value_be = ((int(value_bytes[0], 16) << 24) +
                               (int(value_bytes[1], 16) << 16) +
                               (int(value_bytes[2], 16) << 8) +
                               int(value_bytes[3], 16))
                    
                    # Use little endian by default (more common in embedded systems)
                    # But store both for comparison
                    value = value_le
                    
                    return {
                        'type': f'30-{length:02d}',
                        'data_id': '982D',  # Fixed ID for this special field
                        'data_type': '08',
                        'value': value,
                        'value_le': value_le,  # Little endian interpretation
                        'value_be': value_be,  # Big endian interpretation
                        'raw_bytes': '-'.join(value_bytes),  # Raw bytes for debugging
                        'raw': message
                    }
                else:
                    # Fallback if not exactly 4 bytes
                    value = 0
                    for i, byte in enumerate(value_bytes):
                        value += int(byte, 16) << (8 * i)
                    
                    return {
                        'type': f'30-{length:02d}',
                        'data_id': '982D',
                        'data_type': '08',
                        'value': value,
                        'raw': message
                    }
            
            else:
                # Standard handling: only last byte is the value
                if len(bytes_data) >= 6:
                    # Everything except the last byte is identifier/metadata
                    data_id_bytes = bytes_data[2:-1]  # All bytes except last
                    data_id = ''.join(data_id_bytes)
                    
                    # Extract data type (usually the byte before the value)
                    data_type = bytes_data[-2] if len(bytes_data) >= 6 else None
                    
                    # Value is always the last byte
                    value = int(bytes_data[-1], 16)
                    
                    return {
                        'type': f'30-{length:02d}',
                        'data_id': data_id,
                        'data_type': data_type,
                        'value': value,
                        'raw': message
                    }
                
        # For any other format, capture what we can
        return {
            'type': f'30-{length:02d}',
            'data_id': 'unknown',
            'data_type': 'unknown',
            'value': 0,
            'raw': message
        }
    
    def add_data(self, hex_data):
        """Add hex data line and parse all messages in it"""
        messages = self.parse_hex_data(hex_data)
        
        for msg in messages:
            parsed = self.parse_message(msg)
            if parsed:
                self.messages.append(parsed)
                
                # Store by data ID
                key = f"{parsed['data_id']}"
                if parsed['data_type']:
                    key += f"_{parsed['data_type']}"
                    
                self.data_by_id[key].append(parsed['value'])
                self.data_types[parsed['data_type']] += 1
                
                # Update statistics
                stats = self.id_stats[key]
                stats['count'] += 1
                stats['values'].append(parsed['value'])
                if stats['min'] is None or parsed['value'] < stats['min']:
                    stats['min'] = parsed['value']
                if stats['max'] is None or parsed['value'] > stats['max']:
                    stats['max'] = parsed['value']
    
    def load_from_file(self, filename):
        """Load hex data from file, one message per line"""
        try:
            with open(filename, 'r') as f:
                for line_num, line in enumerate(f, 1):
                    line = line.strip()
                    if line and not line.startswith('#'):  # Skip empty lines and comments
                        try:
                            self.add_data(line)
                        except Exception as e:
                            print(f"Error parsing line {line_num}: {line}")
                            print(f"Error: {e}")
            print(f"Successfully loaded data from {filename}")
        except FileNotFoundError:
            print(f"Error: File '{filename}' not found")
        except Exception as e:
            print(f"Error reading file: {e}")
    
    def print_summary(self):
        """Print analysis summary"""
        print("=== BLE Message Analysis Summary ===\n")
        
        print(f"Total messages parsed: {len(self.messages)}")
        print(f"Unique data IDs found: {len(self.data_by_id)}")
        print()
        
        print("Data Types frequency:")
        for dtype, count in self.data_types.most_common():
            if dtype:
                print(f"  {dtype}: {count} times")
        print()
        
        print("Data ID Statistics:")
        print(f"{'Data ID':<20} {'Count':<8} {'Min':<8} {'Max':<8} {'Range':<10} {'Last 5 Values'}")
        print("-" * 80)
        
        for data_id, stats in sorted(self.id_stats.items()):
            if stats['count'] > 0:
                range_val = stats['max'] - stats['min'] if stats['min'] is not None else 0
                recent_values = stats['values'][-5:]
                print(f"{data_id:<20} {stats['count']:<8} {stats['min']:<8} {stats['max']:<8} {range_val:<10} {recent_values}")
    
    def plot_data(self, max_plots=12):
        """Plot time series for each data ID"""
        if not HAS_MATPLOTLIB:
            print("Matplotlib not available. Install with: pip install matplotlib")
            print("Alternatively, use the CSV export to plot in Excel/other tools.")
            return
            
        # Filter out IDs with too few data points or no variation
        interesting_ids = {}
        for data_id, values in self.data_by_id.items():
            if len(values) >= 3:  # At least 3 data points
                if len(set(values)) > 1:  # Has variation
                    interesting_ids[data_id] = values
        
        if not interesting_ids:
            print("No interesting data series found to plot.")
            return
            
        # Limit number of plots
        plot_ids = list(interesting_ids.keys())[:max_plots]
        
        # Calculate grid size
        n_plots = len(plot_ids)
        cols = min(3, n_plots)
        rows = (n_plots + cols - 1) // cols
        
        fig, axes = plt.subplots(rows, cols, figsize=(15, 4*rows))
        if n_plots == 1:
            axes = [axes]
        elif rows == 1:
            axes = [axes] if cols == 1 else axes
        else:
            axes = axes.flatten()
        
        for i, data_id in enumerate(plot_ids):
            ax = axes[i] if n_plots > 1 else axes[0]
            values = interesting_ids[data_id]
            
            ax.plot(values, 'o-', markersize=3)
            ax.set_title(f'Data ID: {data_id}')
            ax.set_xlabel('Message Index')
            ax.set_ylabel('Value')
            ax.grid(True, alpha=0.3)
            
            # Add statistics to plot
            stats = self.id_stats[data_id]
            ax.text(0.02, 0.98, f"Range: {stats['min']}-{stats['max']}\nCount: {stats['count']}", 
                   transform=ax.transAxes, verticalalignment='top',
                   bbox=dict(boxstyle='round', facecolor='wheat', alpha=0.5))
        
        # Hide unused subplots
        for i in range(n_plots, len(axes)):
            axes[i].set_visible(False)
            
        plt.tight_layout()
        plt.show()
    
    def export_csv(self, filename="ble_data.csv"):
        """Export parsed data to CSV"""
        if HAS_PANDAS:
            df = pd.DataFrame(self.messages)
            df.to_csv(filename, index=False)
        else:
            # Basic CSV export without pandas
            with open(filename, 'w') as f:
                f.write("type,data_id,data_type,value,raw\n")
                for msg in self.messages:
                    f.write(f"{msg['type']},{msg['data_id']},{msg['data_type']},{msg['value']},{msg['raw']}\n")
        print(f"Data exported to {filename}")

# Example usage
if __name__ == "__main__":
    import sys
    
    analyzer = BLEMessageAnalyzer()
    
    if len(sys.argv) > 1:
        # Load data from file
        filename = sys.argv[1]
        analyzer.load_from_file(filename)
    else:
        # Use sample data if no file provided
        sample_data = [
            "30-02-98-09",
            "30-07-98-08-08-F4-09-10-01-30-07-98-2D-08-F4-09-10-01",
            "30-02-98-09-30-04-A2-43-08-11-30-05-A2-4A-08-9B-01-30-04-A2-54-08-2E-30-04-A2-51-08-02-30-04-A2-48-08-6E-30-09-A2-52-0A-05-11-00-02-00-11",
            "30-07-98-18-08-AF-D3-C0-01",
            "30-07-98-08-08-A8-08-10-01-30-07-98-2D-08-D1-08-10-01",
            "30-02-98-09",
            "30-07-98-18-08-B1-D3-C0-01",
            "30-07-98-08-08-9B-09-10-01-30-07-98-2D-08-9B-09-10-01",
            "30-04-98-5A-08-76-30-04-A2-48-08-70-30-05-98-14-08-F2-01-30-04-98-5B-08-4B-30-05-A2-4A-08-93-01-30-04-A2-43-08-12-30-05-A2-4A-08-93-01",
            "30-02-98-09-30-04-A2-54-08-2F-30-04-A2-51-08-02-30-04-A2-48-08-70-30-09-A2-52-0A-05-15-00-02-00-11",
            "30-0B-98-74-08-D8-04-10-D8-04-18-D8-04-30-04-98-09-08-01-30-09-A2-52-0A-05-15-00-02-00-11"
        ]
        
        print("No file provided, using sample data...")
        print("Usage: python script.py <hex_data_file.txt>")
        print()
        
        for line in sample_data:
            analyzer.add_data(line)
    
    # Analyze results
    analyzer.print_summary()
    analyzer.plot_data()
    
    # Optionally export to CSV
    if len(sys.argv) > 1:
        csv_filename = sys.argv[1].replace('.txt', '_analyzed.csv')
        analyzer.export_csv(csv_filename)