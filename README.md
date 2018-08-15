# LeanIX Data Automation
1.	IT Components associated with Behavior should also be associated with the Behavior Provider
2.	Data Objects associated with Behavior should also be associated with the Behavior Provider

## Requirements
* Java 8

## Usage
To generate an API token, you must be an administrator in LeanIX. From the home page, click on your picture in the top right -> Administration -> API Tokens (under Developers) -> New Token.

### Automation
`$ java -jar main.jar {API token}`

### Undo Changes Made on Last Fun
`$ java -jar reset.jar {API token}`

## Testing
Tests are currently made specifically for EBSCO's SBEIS workspace, as they make use of fact sheet IDs specific to that location.
