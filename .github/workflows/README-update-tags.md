# Version Tag Update Workflow

This GitHub workflow automates the process of updating version tags for RDK WAN components in the meta-rdk-wan repository.

## Access Control

This workflow can be triggered by any user with write access to the repository.

## Supported Components

The workflow can update version tags for the following components:

| Component | Repository | BitBake Recipe |
|-----------|------------|----------------|
| PPP Manager | `github.com/rdkcentral/ppp-manager` | `recipes-ccsp/ccsp/rdk-ppp-manager.bb` |
| VLAN Manager | `github.com/rdkcentral/vlan-manager` | `recipes-ccsp/ccsp/rdk-vlanmanager.bb` |
| WAN Manager | `github.com/rdkcentral/wan-manager` | `recipes-ccsp/ccsp/rdk-wanmanager.bb` |
| GPON Manager | `github.com/rdkcentral/gpon-manager` | `recipes-ccsp/ccsp/rdkgponmanager.bb` |
| xDSL Manager | `github.com/rdkcentral/xdsl-manager` | `recipes-ccsp/ccsp/rdkxdslmanager.bb` |
| IPoE Health Check | `github.com/rdkcentral/ipoe-health-check` | `recipes-support/ipoe-health-check/ipoe-health-check.bb` |

## How to Use

1. **Navigate to Actions**: Go to the GitHub repository's Actions tab
2. **Select Workflow**: Find and click on "Update Version Tags" workflow
3. **Run Workflow**: Click "Run workflow" button
4. **Fill Parameters**:
   - **Source Branch**: The branch to create the PR from (default: `main`)
   - **Component Tags**: Enter version tags for the components you want to update (e.g., `v1.5.0`)
   
   You can update one or multiple components in a single run.

## What the Workflow Does

1. **Input Validation**: Validates tag formats and ensures at least one component is specified
2. **Branch Creation**: Creates a new branch with timestamp: `update-tags-YYYYMMDD-HHMMSS`
3. **Smart Branch Detection**: For each specified component:
   - Queries GitHub API to find which branch contains the tag
   - Falls back to existing branch in BitBake file if API fails
   - Uses version-based pattern as final fallback
4. **Tag Updates**: For each specified component:
   - Updates the `#GIT_TAG` line with the new version
   - Updates the `#SRC_URI` line with the automatically detected branch
   - Updates the `#PV` line with the tag reference
5. **Changelog Generation**: Creates a summary of changes between versions
6. **Commit**: Commits all changes with a descriptive message
7. **Pull Request**: Creates a PR with:
   - Detailed description of updated components
   - Links to release comparisons
   - Testing checklist

## Example

To update WAN Manager to version `v2.11.0` and xDSL Manager to `v1.5.0`:

1. Run the workflow
2. Set `source_branch` to `main`
3. Set `wan_manager_tag` to `v2.11.0`
4. Set `xdsl_manager_tag` to `v1.5.0`
5. Leave other component tags empty

## BitBake Recipe Changes

The workflow modifies the commented release lines in each BitBake recipe file:

**Before:**
```bitbake
# Please use below part only for official release and release candidates
#GIT_TAG = "v1.4.0"
#SRC_URI := "git://github.com/rdkcentral/xdsl-manager.git;branch=releases/1.4.0-main;protocol=https;name=xDSLManager;tag=${GIT_TAG}"
#PV = "${GIT_TAG}+git${SRCPV}"
```

**After (for v1.5.0):**
```bitbake
# Please use below part only for official release and release candidates
#GIT_TAG = "v1.5.0"
#SRC_URI := "git://github.com/rdkcentral/xdsl-manager.git;branch=releases/1.5.0-main;protocol=https;name=xDSLManager;tag=${GIT_TAG}"
#PV = "${GIT_TAG}+git${SRCPV}"
```

## Notes

- The workflow **automatically detects the correct branch** for each tag using GitHub API
- Multiple fallback mechanisms ensure reliability even if API queries fail
- Version tags should follow semantic versioning (e.g., `v1.5.0`)
- The workflow creates a PR that is ready for review and requires manual approval before merging
- Make sure the specified tags exist in their respective repositories before running

## Local Testing

You can test the update logic locally before running the GitHub workflow:

```bash
# Test individual components
python3 .github/scripts/test_component_updates.py --wan-manager v2.12.0 --xdsl-manager v1.6.0

# Test all components with the same version
python3 .github/scripts/test_component_updates.py --all-components v1.6.0

# List all available components
python3 .github/scripts/test_component_updates.py --list-components

# Test a single component update
python3 .github/scripts/update_component.py --dry-run v2.11.0 wan-manager recipes-ccsp/ccsp/rdk-wanmanager.bb WanManager
```

## Troubleshooting

- **No Changes**: Verify that the specified tags are different from the current ones in the bb files
- **Branch Not Found**: Ensure the source branch exists and you have access to it
- **Tag Not Found**: Verify that the version tags exist in the respective component repositories