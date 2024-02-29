import csv

def csv_to_dict(csv_file_path):
    """
    Reads a CSV file and returns a dictionary with the first field of each row as the key,
    assuming that the CSV uses ':' as its field delimiter.
    
    Parameters:
    - csv_file_path: Path to the CSV file
    
    Returns:
    - A dictionary where each key is the first field of a row, and the value is the rest of the row as a list.
    """
    result_dict = {}
    with open(csv_file_path, mode='r', encoding='utf-8') as file:
        # Specify ':' as the delimiter
        csv_reader = csv.reader(file, delimiter=':')
        for row in csv_reader:
            if row:  # Ensure the row is not empty
                key = row[0]
                value = row[1:]  # All other fields in the row
                result_dict[key] = value
    return result_dict

# Example usage
csv_file_path = 'path/to/your/csv_file.csv'
result_dict = csv_to_dict(csv_file_path)
print(result_dict)
