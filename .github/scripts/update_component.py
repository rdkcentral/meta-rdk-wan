#!/usr/bin/env python3
"""
Component Version Updater for RDK WAN meta layer.

This script updates BitBake recipe files with new version tags or release branches.

For version tags (official releases):
- Adds/updates GIT_TAG field
- Updates SRC_URI with tag reference
- Adds PV field
- Comments out SRCREV

For release branches (sprint validation):
- Removes GIT_TAG field
- Updates SRC_URI without tag reference
- Replaces PV with SRCREV = "${AUTOREV}"

Usage:
    python3 update_component.py <tag_or_branch> <repo> <bb_file> <component_name>

Examples:
    # Official release:
    python3 update_component.py v2.11.0 wan-manager recipes-ccsp/ccsp/rdk-wanmanager.bb WanManager
    
    # Sprint validation:
    python3 update_component.py releases/1.4.0-main ipoe-health-check recipes-support/ipoe-health-check/ipoe-health-check.bb IPoEHealthCheck
"""

import argparse
import json
import re
import subprocess
import sys
import urllib.request
import urllib.error
from pathlib import Path
from typing import Optional, List


class ComponentUpdater:
    """Handles updating BitBake recipe files with new version tags."""
    
    def __init__(self, github_org: str = "rdkcentral"):
        self.github_org = github_org
        self.api_base = "https://api.github.com"
        
    def find_tag_branch(self, repo: str, tag: str) -> Optional[str]:
        """
        Find the branch that contains a specific tag.
        
        Args:
            repo: Repository name
            tag: Git tag name
            
        Returns:
            Branch name if found, None otherwise
        """
        print(f"🔍 Finding branch for tag {tag} in {self.github_org}/{repo}...")
        
        try:
            # Method 1: Use GitHub API to get tag object
            tag_sha = self._get_tag_sha(repo, tag)
            if tag_sha:
                commit_sha = self._get_commit_sha(repo, tag_sha)
                if commit_sha:
                    branch = self._find_branch_containing_commit(repo, commit_sha)
                    if branch:
                        print(f"✅ Found branch: {branch}")
                        return branch
            
            # Method 2: Fallback to pattern matching
            print("⚠️  API method failed, trying pattern matching...")
            branch = self._find_branch_by_pattern(repo, tag)
            if branch:
                print(f"✅ Found branch by pattern: {branch}")
                return branch
                
        except Exception as e:
            print(f"❌ Error finding branch: {e}")
            
        return None
    
    def _get_tag_sha(self, repo: str, tag: str) -> Optional[str]:
        """Get the SHA of a tag."""
        url = f"{self.api_base}/repos/{self.github_org}/{repo}/git/refs/tags/{tag}"
        try:
            data = self._api_request(url)
            return data.get('object', {}).get('sha')
        except:
            return None
    
    def _get_commit_sha(self, repo: str, tag_sha: str) -> Optional[str]:
        """Get the actual commit SHA (handles annotated tags)."""
        url = f"{self.api_base}/repos/{self.github_org}/{repo}/git/tags/{tag_sha}"
        try:
            data = self._api_request(url)
            # If it's an annotated tag, get the actual commit
            return data.get('object', {}).get('sha', tag_sha)
        except:
            # Assume it's a lightweight tag
            return tag_sha
    
    def _find_branch_containing_commit(self, repo: str, commit_sha: str) -> Optional[str]:
        """Find release branches containing a specific commit."""
        # Get all branches
        url = f"{self.api_base}/repos/{self.github_org}/{repo}/branches"
        try:
            branches_data = self._api_request(url)
            release_branches = [b['name'] for b in branches_data if b['name'].startswith('releases/')]
            
            # Check which branches contain the commit
            for branch in release_branches:
                if self._branch_contains_commit(repo, branch, commit_sha):
                    return branch
                    
        except Exception as e:
            print(f"Error checking branches: {e}")
            
        return None
    
    def _branch_contains_commit(self, repo: str, branch: str, commit_sha: str) -> bool:
        """Check if a branch contains a specific commit."""
        url = f"{self.api_base}/repos/{self.github_org}/{repo}/compare/{branch}...{commit_sha}"
        try:
            data = self._api_request(url)
            status = data.get('status', '')
            return status in ['identical', 'behind']
        except:
            return False
    
    def _find_branch_by_pattern(self, repo: str, tag: str) -> Optional[str]:
        """Find branch using common naming patterns."""
        version = tag.lstrip('v')
        major_minor = '.'.join(version.split('.')[:2])
        
        patterns = [
            f"releases/{major_minor}.0-main",
            f"releases/{version}-main", 
            f"releases/{major_minor}-main",
            f"release-{major_minor}",
            f"release-{version}"
        ]
        
        for pattern in patterns:
            if self._branch_exists(repo, pattern):
                return pattern
                
        return None
    
    def _branch_exists(self, repo: str, branch: str) -> bool:
        """Check if a branch exists."""
        url = f"{self.api_base}/repos/{self.github_org}/{repo}/branches/{branch}"
        try:
            self._api_request(url)
            return True
        except:
            return False
    
    def _api_request(self, url: str) -> dict:
        """Make a GitHub API request."""
        with urllib.request.urlopen(url) as response:
            return json.loads(response.read())
    
    def get_current_branch_from_bb(self, bb_file: Path, repo: str) -> Optional[str]:
        """Extract current branch from BB file's SRC_URI."""
        if not bb_file.exists():
            return None
            
        content = bb_file.read_text()
        pattern = rf"^#SRC_URI.*github\.com/{self.github_org}/{repo}.*?branch=([^;]+)"
        
        for line in content.split('\n'):
            match = re.search(pattern, line)
            if match:
                return match.group(1)
                
        return None
    
    def update_bb_file(self, tag_or_branch: str, repo: str, bb_file: Path, component_name: str) -> bool:
        """
        Update a BitBake recipe file with new version tag or release branch.
        
        Args:
            tag_or_branch: Version tag (e.g., v2.11.0) or release branch (e.g., releases/1.3.0-main)
            repo: Repository name
            bb_file: Path to BitBake file
            component_name: Component name for SRC_URI
            
        Returns:
            True if updated successfully, False otherwise
        """
        if not bb_file.exists():
            print(f"❌ BB file not found: {bb_file}")
            return False
            
        # Determine if this is a version tag or release branch
        is_version_tag = tag_or_branch.startswith('v') and re.match(r'^v\d+\.\d+\.\d+', tag_or_branch)
        is_release_branch = tag_or_branch.startswith('releases/') and tag_or_branch.endswith('-main')
        
        if not is_version_tag and not is_release_branch:
            print(f"❌ Invalid format: {tag_or_branch}")
            print("Expected: version tag (v1.3.0) or release branch (releases/1.3.0-main)")
            return False
        
        print(f"🔄 Updating {component_name} with {'tag' if is_version_tag else 'branch'} {tag_or_branch}...")
        
        # Read current content
        content = bb_file.read_text()
        
        if is_version_tag:
            # Handle version tag - official release
            tag = tag_or_branch
            
            # Find the correct branch
            branch = self.find_tag_branch(repo, tag)
            
            if not branch:
                print("⚠️  Branch detection failed, trying fallback methods...")
                
                # Try to extract from existing BB file
                branch = self.get_current_branch_from_bb(bb_file, repo)
                
                if branch:
                    print(f"📄 Using current branch from BB file: {branch}")
                else:
                    # Final fallback: construct from version
                    version = tag.lstrip('v')
                    branch_version = '.'.join(version.split('.')[:2])
                    branch = f"releases/{branch_version}.0-main"
                    print(f"🔄 Using fallback branch pattern: {branch}")
            
            # Add or update GIT_TAG if not found
            if 'GIT_TAG = ' not in content:
                # Find the right place to insert GIT_TAG (after LIC_FILES_CHKSUM, before SRC_URI)
                lines = content.split('\n')
                insert_idx = -1
                for i, line in enumerate(lines):
                    if 'LIC_FILES_CHKSUM' in line:
                        insert_idx = i + 1
                        break
                    elif 'DEPENDS' in line and insert_idx == -1:
                        insert_idx = i + 1
                
                if insert_idx > 0:
                    lines.insert(insert_idx, '')
                    lines.insert(insert_idx + 1, '# Please use below part only for official release and release candidates')
                    lines.insert(insert_idx + 2, f'GIT_TAG = "{tag}"')
                    content = '\n'.join(lines)
                else:
                    print("⚠️  Could not find suitable location to insert GIT_TAG")
            else:
                # Update existing GIT_TAG
                content = re.sub(
                    r'GIT_TAG = ".*"',
                    f'GIT_TAG = "{tag}"',
                    content
                )
            
            # Update SRC_URI for version tag (with tag reference)
            new_src_uri = f'SRC_URI := "git://github.com/{self.github_org}/{repo}.git;branch={branch};protocol=https;name={component_name};tag=${{GIT_TAG}}"'
            
            # Replace any existing SRC_URI
            content = re.sub(
                rf'SRC_URI\s*[:=]+\s*"git://github\.com/{self.github_org}/{repo}\.git[^"]*"',
                new_src_uri,
                content
            )
            
            # Add or update PV
            if 'PV = ' not in content:
                content = content.replace(
                    new_src_uri,
                    new_src_uri + '\nPV = "${GIT_TAG}+git${SRCPV}"'
                )
            else:
                content = re.sub(
                    r'PV = ".*"',
                    'PV = "${GIT_TAG}+git${SRCPV}"',
                    content
                )
            
            # Comment out any SRCREV lines
            content = re.sub(
                r'^SRCREV = ',
                '#SRCREV = ',
                content,
                flags=re.MULTILINE
            )
            
        else:
            # Handle release branch - sprint validation
            branch = tag_or_branch
            
            # Remove GIT_TAG lines
            content = re.sub(
                r'^GIT_TAG = ".*"\n?',
                '',
                content,
                flags=re.MULTILINE
            )
            
            # Update SRC_URI for branch (without tag reference)
            new_src_uri = f'SRC_URI = "git://github.com/{self.github_org}/{repo}.git;branch={branch};protocol=https;name={component_name};"'
            
            # Replace any existing SRC_URI
            content = re.sub(
                rf'SRC_URI\s*[:=]+\s*"git://github\.com/{self.github_org}/{repo}\.git[^"]*"',
                new_src_uri,
                content
            )
            
            # Replace PV with SRCREV
            content = re.sub(
                r'PV = "\$\{GIT_TAG\}\+git\$\{SRCPV\}"',
                'SRCREV = "${AUTOREV}"',
                content
            )
            
            # Uncomment SRCREV if it was commented
            content = re.sub(
                r'^#SRCREV = ',
                'SRCREV = ',
                content,
                flags=re.MULTILINE
            )
            
            # Add SRCREV if not found
            if 'SRCREV = ' not in content:
                content = content.replace(
                    new_src_uri,
                    new_src_uri + '\nSRCREV = "${AUTOREV}"'
                )
        
        # Clean up any extra blank lines
        content = re.sub(r'\n{3,}', '\n\n', content)
        
        # Write back to file
        bb_file.write_text(content)
        
        print(f"✅ Updated {bb_file} with {'tag' if is_version_tag else 'branch'} {tag_or_branch}")
        return True


def main():
    """Main entry point."""
    parser = argparse.ArgumentParser(
        description="Update RDK WAN component version tags or release branches in BitBake recipes",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
  # Official release with version tag:
  %(prog)s v2.11.0 wan-manager recipes-ccsp/ccsp/rdk-wanmanager.bb WanManager
  
  # Sprint validation with release branch:
  %(prog)s releases/1.4.0-main ipoe-health-check recipes-support/ipoe-health-check/ipoe-health-check.bb IPoEHealthCheck
        """
    )
    
    parser.add_argument('tag_or_branch', help='Version tag (e.g., v2.11.0) or release branch (e.g., releases/1.4.0-main)')
    parser.add_argument('repo', help='Repository name (e.g., wan-manager)')
    parser.add_argument('bb_file', help='Path to BitBake recipe file')
    parser.add_argument('component_name', help='Component name for SRC_URI')
    parser.add_argument('--org', default='rdkcentral', help='GitHub organization (default: rdkcentral)')
    parser.add_argument('--dry-run', action='store_true', help='Show what would be done without making changes')
    
    args = parser.parse_args()
    
    # Validate input format
    is_version_tag = args.tag_or_branch.startswith('v') and re.match(r'^v\d+\.\d+\.\d+', args.tag_or_branch)
    is_release_branch = args.tag_or_branch.startswith('releases/') and args.tag_or_branch.endswith('-main')
    
    if not is_version_tag and not is_release_branch:
        print(f"❌ Invalid format: {args.tag_or_branch}")
        print("Expected formats:")
        print("  Version tag: v1.3.0")
        print("  Release branch: releases/1.3.0-main")
        return 1
    
    # Create updater
    updater = ComponentUpdater(args.org)
    bb_file = Path(args.bb_file)
    
    if args.dry_run:
        print("🔍 DRY RUN MODE - No changes will be made")
        if is_version_tag:
            branch = updater.find_tag_branch(args.repo, args.tag_or_branch)
            if not branch:
                branch = updater.get_current_branch_from_bb(bb_file, args.repo) or "fallback-pattern"
            print(f"Would update {bb_file} with tag {args.tag_or_branch} on branch {branch}")
        else:
            print(f"Would update {bb_file} with release branch {args.tag_or_branch}")
        return 0
    
    # Perform the update
    success = updater.update_bb_file(args.tag_or_branch, args.repo, bb_file, args.component_name)
    return 0 if success else 1


if __name__ == '__main__':
    sys.exit(main())