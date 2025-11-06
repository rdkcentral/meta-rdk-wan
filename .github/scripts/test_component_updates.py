#!/usr/bin/env python3
"""
Test script for the Component Version Updater.

This script validates the update logic without making actual changes to files.
It's useful for testing before running the actual GitHub workflow.

Usage:
    python3 test_component_updates.py [options]

Examples:
    # Test with version tags (official releases):
    python3 test_component_updates.py --wan-manager v2.11.0 --xdsl-manager v1.5.0
    
    # Test with release branches (sprint validation):
    python3 test_component_updates.py --wan-manager releases/2.11.0-main
    
    # Test all components with same version:
    python3 test_component_updates.py --all-components v1.5.0
"""

import argparse
import sys
from pathlib import Path
from typing import Dict, List

# Import our component updater
try:
    from update_component import ComponentUpdater
except ImportError:
    # Add the scripts directory to path
    import os
    sys.path.insert(0, os.path.dirname(__file__))
    from update_component import ComponentUpdater


class ComponentTester:
    """Test harness for component updates."""
    
    # Component mapping table
    COMPONENTS = {
        'ppp-manager': {
            'bb_file': 'recipes-ccsp/ccsp/rdk-ppp-manager.bb',
            'name': 'PPPManager',
            'display': 'PPP Manager'
        },
        'vlan-manager': {
            'bb_file': 'recipes-ccsp/ccsp/rdk-vlanmanager.bb', 
            'name': 'VLANManager',
            'display': 'VLAN Manager'
        },
        'wan-manager': {
            'bb_file': 'recipes-ccsp/ccsp/rdk-wanmanager.bb',
            'name': 'WanManager', 
            'display': 'WAN Manager'
        },
        'gpon-manager': {
            'bb_file': 'recipes-ccsp/ccsp/rdkgponmanager.bb',
            'name': 'GPONManager',
            'display': 'GPON Manager'
        },
        'xdsl-manager': {
            'bb_file': 'recipes-ccsp/ccsp/rdkxdslmanager.bb',
            'name': 'xDSLManager',
            'display': 'xDSL Manager'
        },
        'ipoe-health-check': {
            'bb_file': 'recipes-support/ipoe-health-check/ipoe-health-check.bb',
            'name': 'IPoEHealthCheck',
            'display': 'IPoE Health Check'
        }
    }
    
    def __init__(self):
        self.updater = ComponentUpdater()
        self.errors = 0
        self.warnings = 0
        
    def print_status(self, status: str, message: str):
        """Print colored status messages."""
        colors = {
            'success': '\033[0;32m✅',
            'error': '\033[0;31m❌', 
            'warning': '\033[1;33m⚠️',
            'info': '\033[0;34mℹ️'
        }
        reset = '\033[0m'
        
        color = colors.get(status, '')
        print(f"{color} {message}{reset}")
        
        if status == 'error':
            self.errors += 1
        elif status == 'warning':
            self.warnings += 1
    
    def validate_input_format(self, input_value: str, component: str) -> bool:
        """Validate input format (version tag or release branch)."""
        import re
        
        is_version_tag = input_value.startswith('v') and re.match(r'^v\d+\.\d+\.\d+', input_value)
        is_release_branch = input_value.startswith('releases/') and input_value.endswith('-main')
        
        if not is_version_tag and not is_release_branch:
            self.print_status('error', f"Invalid format for {component}: {input_value}")
            self.print_status('info', "Expected formats: v1.3.0 (version tag) or releases/1.3.0-main (release branch)")
            return False
        elif is_version_tag:
            self.print_status('success', f"Valid version tag for {component}: {input_value}")
        else:
            self.print_status('success', f"Valid release branch for {component}: {input_value}")
        
        return True
    
    def test_component_update(self, repo: str, tag_or_branch: str, dry_run: bool = True) -> bool:
        """Test updating a single component."""
        if repo not in self.COMPONENTS:
            self.print_status('error', f"Unknown component: {repo}")
            return False
            
        config = self.COMPONENTS[repo]
        bb_file = Path(config['bb_file'])
        
        input_type = "tag" if tag_or_branch.startswith('v') else "branch"
        self.print_status('info', f"Testing update for {config['display']} with {input_type} {tag_or_branch}")
        
        # Check if BB file exists
        if not bb_file.exists():
            self.print_status('error', f"BB file not found: {bb_file}")
            return False
            
        # Show current tag in file
        current_tag = self._get_current_tag(bb_file)
        if current_tag:
            self.print_status('info', f"Current tag in {bb_file}: {current_tag}")
            if current_tag == tag_or_branch:
                self.print_status('warning', f"Tag {tag_or_branch} is already current in {bb_file}")
        else:
            self.print_status('info', f"No current tag found in {bb_file}")
        
        # Test branch detection for version tags only
        if tag_or_branch.startswith('v'):
            branch = self.updater.find_tag_branch(repo, tag_or_branch)
            if not branch:
                self.print_status('warning', "Could not determine branch from repository, using fallback")
                branch = self.updater.get_current_branch_from_bb(bb_file, repo)
                if branch:
                    self.print_status('info', f"Using current branch from BB file: {branch}")
                else:
                    version = tag_or_branch.lstrip('v')
                    branch_version = '.'.join(version.split('.')[:2])
                    branch = f"releases/{branch_version}.0-main"
                    self.print_status('info', f"Using fallback branch pattern: {branch}")
            
            self.print_status('info', f"Would use branch: {branch}")
        else:
            self.print_status('info', f"Using provided release branch: {tag_or_branch}")
        
        if not dry_run:
            # Actually perform the update
            success = self.updater.update_bb_file(tag_or_branch, repo, bb_file, config['name'])
            return success
        
        return True
    
    def _get_current_tag(self, bb_file: Path) -> str:
        """Extract current tag from BB file."""
        import re
        content = bb_file.read_text()
        match = re.search(r'^#GIT_TAG = "([^"]*)"', content, re.MULTILINE)
        return match.group(1) if match else ""
    
    def run_tests(self, component_tags: Dict[str, str], dry_run: bool = True) -> bool:
        """Run tests for multiple components."""
        self.print_status('info', "Starting component version update tests...")
        
        # Check if we're in the right directory
        if not Path("recipes-ccsp/ccsp/rdk-wanmanager.bb").exists():
            self.print_status('error', "Not in meta-rdk-wan repository root. Please run from repository root.")
            return False
        
        # Validate all inputs first
        for repo, tag_or_branch in component_tags.items():
            if not self.validate_input_format(tag_or_branch, self.COMPONENTS[repo]['display']):
                continue
        
        if self.errors > 0:
            self.print_status('error', f"Validation failed with {self.errors} error(s)")
            return False
            
        self.print_status('success', "All inputs validated successfully")
        
        # Test each component update
        self.print_status('info', f"Testing component updates ({'dry run' if dry_run else 'live run'})...")
        
        for repo, tag_or_branch in component_tags.items():
            self.test_component_update(repo, tag_or_branch, dry_run)
        
        # Summary
        if self.errors == 0:
            self.print_status('success', "All tests completed successfully!")
            if dry_run:
                self.print_status('info', "This was a dry run. No files were modified.")
        else:
            self.print_status('error', f"Tests completed with {self.errors} error(s) and {self.warnings} warning(s)")
            
        return self.errors == 0


def main():
    """Main entry point."""
    parser = argparse.ArgumentParser(
        description="Test RDK WAN component version tag updates",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
  %(prog)s --wan-manager v2.11.0 --xdsl-manager v1.5.0
  %(prog)s --all-components v1.6.0
  %(prog)s --ppp-manager v1.5.0 --live-run
        """
    )
    
    # Component-specific arguments
    for repo, config in ComponentTester.COMPONENTS.items():
        parser.add_argument(
            f'--{repo}',
            metavar='TAG',
            help=f"{config['display']} version tag (e.g., v1.5.0)"
        )
    
    # Special options
    parser.add_argument(
        '--all-components',
        metavar='TAG', 
        help='Use the same tag for all components'
    )
    
    parser.add_argument(
        '--live-run',
        action='store_true',
        help='Actually modify files (default is dry-run)'
    )
    
    parser.add_argument(
        '--list-components',
        action='store_true',
        help='List all available components and exit'
    )
    
    args = parser.parse_args()
    
    # Handle special cases
    if args.list_components:
        print("Available components:")
        for repo, config in ComponentTester.COMPONENTS.items():
            print(f"  --{repo:<20} {config['display']}")
        return 0
    
    # Collect component tags
    component_tags = {}
    
    if args.all_components:
        # Use the same tag for all components
        for repo in ComponentTester.COMPONENTS.keys():
            component_tags[repo] = args.all_components
    else:
        # Collect individual component tags
        for repo in ComponentTester.COMPONENTS.keys():
            tag = getattr(args, repo.replace('-', '_'))
            if tag:
                component_tags[repo] = tag
    
    if not component_tags:
        parser.error("No version tags provided. Specify at least one component or use --all-components.")
    
    # Run the tests
    tester = ComponentTester()
    success = tester.run_tests(component_tags, dry_run=not args.live_run)
    
    return 0 if success else 1


if __name__ == '__main__':
    sys.exit(main())