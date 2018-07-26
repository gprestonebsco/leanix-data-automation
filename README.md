# LeanIX Data Automation
1.	IT Components associated with Behavior should also be associated with the Behavior Provider
2.	Data Objects associated with Behavior should also be associated with the Behavior Provider

## Requirements
* Java 8

## Usage
### Automation
`$ java -jar main.jar -t {API token}`

`$ java -jar main.jar -f {API token file path}`

### Undo Changes Made on Last Fun
`$ java -jar reset.jar -t {API token}`

`$ java -jar reset.jar -f {API token file path}`

## Testing
Tests are currently made specifically for EBSCO's SBEIS workspace, as they make use of fact sheet IDs specific to that location.
