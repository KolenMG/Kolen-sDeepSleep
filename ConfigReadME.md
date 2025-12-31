# Kolen's DeepSleep - Setup Instructions

## 📋 Project Structure Created!

All folders and files have been created. Now you need to populate them with content.

## 🔢 Artifact Reference Guide

Each file has a comment indicating which artifact to paste. Here's the complete list:

### Configuration Files
- **pom.xml** → Artifact #1 (pom_xml)
- **plugin.yml** → Artifact #2 (plugin_yml)
- **config.yml** → Artifact #3 (config_yml)
- **messages.yml** → Artifact #4 (messages_yml)

### Utility Classes
- **BedData.java** → Artifact #5 (bed_data)
- **ConfigUtil.java** → Artifact #6 (config_util)
- **MessageUtil.java** → Artifact #7 (message_util)

### Validators
- **BedPlacementValidator.java** → Artifact #8 (bed_validator)

### Managers
- **CooldownManager.java** → Artifact #9 (cooldown_manager)
- **BedManager.java** → Artifact #10 (bed_manager)
- **SleepManager.java** → Artifact #11 (sleep_manager)
- **AnimationManager.java** → Artifact #12 (animation_manager)

### Listeners
- **CombatListener.java** → Artifact #13 (combat_listener)
- **PlayerListener.java** → Artifact #15 (player_listener)
- **BedListener.java** → Artifact #16 (bed_listener)

### Commands
- **BedCommand.java** → Artifact #17 (bed_command)
- **AdminCommand.java** → Artifact #18 (admin_command)

### Hooks
- **HookManager.java** → Artifact #19 (hook_manager)
- **WorldGuardHook.java** → Artifact #20 (worldguard_hook)
- **PlaceholderAPIHook.java** → Artifact #21 (placeholderapi_hook)

### Main Class
- **KolensDeepSleep.java** → Artifact #14 (main_plugin_class)

## 📝 How to Populate Files

1. Open each file in your favorite text editor
2. Find the `TODO:` comment
3. Replace the placeholder with the content from the corresponding artifact
4. Save the file

## 🔨 Building the Plugin

After populating all files:

```bash
cd KolensDeepSleep
mvn clean package
```

The compiled JAR will be in: `target/KolensDeepSleep-1.0.0.jar`

## 📁 Project Structure

```
KolensDeepSleep/
├── pom.xml
├── README.md (this file)
└── src/main/
    ├── java/com/kolensdeepsleep/
    │   ├── KolensDeepSleep.java (Main class)
    │   ├── commands/
    │   │   ├── BedCommand.java
    │   │   └── AdminCommand.java
    │   ├── managers/
    │   │   ├── BedManager.java
    │   │   ├── SleepManager.java
    │   │   ├── AnimationManager.java
    │   │   └── CooldownManager.java
    │   ├── listeners/
    │   │   ├── PlayerListener.java
    │   │   ├── BedListener.java
    │   │   └── CombatListener.java
    │   ├── hooks/
    │   │   ├── HookManager.java
    │   │   ├── WorldGuardHook.java
    │   │   └── PlaceholderAPIHook.java
    │   ├── util/
    │   │   ├── BedData.java
    │   │   ├── ConfigUtil.java
    │   │   └── MessageUtil.java
    │   └── validators/
    │       └── BedPlacementValidator.java
    └── resources/
        ├── plugin.yml
        ├── config.yml
        └── messages.yml
```

## ✅ Checklist

- [ ] All files populated with content
- [ ] Maven dependencies configured in pom.xml
- [ ] Plugin metadata set in plugin.yml
- [ ] Configuration values set in config.yml
- [ ] Messages configured in messages.yml
- [ ] All Java classes have correct package declarations
- [ ] Project builds successfully with Maven
- [ ] JAR file generated in target/

## 🚀 Next Steps

1. Populate all files with their corresponding artifact content
2. Build the project: `mvn clean package`
3. Test on a Paper 1.21.x server
4. Follow the testing guide (Artifact #22 - final_checklist)

Good luck! 🎮
