module.exports = {
    onboardingConfig: {
      extends: ["config:base"],
    },
    platform: "github",
    onboarding: false,
    includeForks: false,
    branchPrefix: "renovate/",
    gitAuthor: "renovate bot <111297361+hashicorp-vault-sonar-prod[bot]@users.noreply.github.com>",
    username: "hashicorp-vault-sonar-prod[bot]",
    baseBranches: ["master"],
    repositories: [
      "SonarSource/orchestrator",
    ],
    // Renovate configuration
    extends: [
      "config:base",
      ":preserveSemverRanges"
    ],
    branchConcurrentLimit: 0,
    prConcurrentLimit: 0,
    prHourlyLimit: 0,
    separateMinorPatch: false,
    separateMajorMinor: false,
    ignoreDeps: [],
    labels: [
      "dependencies"
    ],
    packageRules: [
      {
        matchManagers: "maven",
        matchUpdateTypes: [
          "patch",
          "minor"
        ],
        enabled: true,
        groupName: "Minor Backend Dependencies"
      },
      {
        matchManagers: "maven",
        matchUpdateTypes: [
          "major"
        ],
        enabled: true,
        groupName: "Major Backend Dependencies"
      },
      {
        matchManagers: "github-actions",
        enabled: false
      },
      {
        matchManagers: "dockerfile",
        enabled: false
      },
      {
        matchManagers: "gradle-wrapper",
        enabled: false
      },
      {
        matchManagers: "gradle",
        enabled: false
      },
      {
        matchManagers: "npm",
        enabled: false
      },
      {
        matchManagers: "mise",
        enabled: false
      }
    ]
  };
