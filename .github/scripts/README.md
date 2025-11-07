# GitHub Scripts Documentation

This directory contains scripts used by the GitHub workflow and for local development/testing.

## Scripts Overview

### 1. `update_component.py`
**Purpose**: Updates BitBake recipe files with new version tags  
**Language**: Python 3  
**Usage**: `python3 update_component.py [options] <tag> <repo> <bb_file> <component_name>`

**Key Features**:
- Automatic branch detection using GitHub API
- Multiple fallback strategies for branch detection
- Handles both `:=` and `=` SRC_URI formats
- Comprehensive error handling and logging
- Dry-run mode for testing

**Examples**:
```bash
# Dry run test
python3 update_component.py --dry-run v2.11.0 wan-manager recipes-ccsp/ccsp/rdk-wanmanager.bb

# Actual update
python3 update_component.py v1.5.0 xdsl-manager recipes-ccsp/ccsp/rdkxdslmanager.bb
```

### 2. `test_component_updates.py`
**Purpose**: Comprehensive testing framework for component updates  
**Language**: Python 3  
**Usage**: `python3 test_component_updates.py [options]`

**Key Features**:
- Tests multiple components in a single run
- Validates tag formats
- Shows current vs. new versions
- Tests branch detection logic
- Colored output for better readability
- Support for testing all components with same version

**Examples**:
```bash
# Test multiple components
python3 test_component_updates.py --wan-manager v2.11.0 --xdsl-manager v1.5.0

# Test all components with same version
python3 test_component_updates.py --all-components v1.6.0

# List available components
python3 test_component_updates.py --list-components

# Perform actual updates (not recommended for testing)
python3 test_component_updates.py --wan-manager v2.11.0 --live-run
```

## Component Mapping

The following components are supported:

| Repository | BitBake File | Component Name | CLI Flag |
|-----------|--------------|----------------|----------|
| `ppp-manager` | `recipes-ccsp/ccsp/rdk-ppp-manager.bb` | `PPPManager` | `--ppp-manager` |
| `vlan-manager` | `recipes-ccsp/ccsp/rdk-vlanmanager.bb` | `VLANManager` | `--vlan-manager` |
| `wan-manager` | `recipes-ccsp/ccsp/rdk-wanmanager.bb` | `WanManager` | `--wan-manager` |
| `gpon-manager` | `recipes-ccsp/ccsp/rdkgponmanager.bb` | `GPONManager` | `--gpon-manager` |
| `xdsl-manager` | `recipes-ccsp/ccsp/rdkxdslmanager.bb` | `xDSLManager` | `--xdsl-manager` |
| `ipoe-health-check` | `recipes-support/ipoe-health-check/ipoe-health-check.bb` | `IPoEHealthCheck` | `--ipoe-health-check` |

## Workflow Integration

The GitHub workflow uses these scripts in the following sequence:

1. **Component Updates**: `update_component.py` updates each specified component with automatic branch detection

## Development Workflow

For local development and testing:

1. **Validate Changes**: Use `test_component_updates.py` to test your changes
2. **Individual Testing**: Use `update_component.py --dry-run` for specific components
3. **Run Workflow**: Execute the GitHub workflow for production updates

## Error Handling

The scripts include comprehensive error handling:
- **API Failures**: Graceful fallback to pattern matching
- **Missing Files**: Clear error messages for missing BitBake files
- **Invalid Tags**: Format validation with helpful error messages
- **Network Issues**: Timeout handling for GitHub API requests

## Dependencies

- **Python Scripts**: Python 3.6+ with standard library only (no external dependencies)