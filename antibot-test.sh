#!/bin/bash

echo "ApiaryAntiBot Test Script"
echo "========================="
echo ""

# First, check if velocity.toml exists and if AntiBot is enabled
if [ -f "velocity.toml" ]; then
  echo "✓ Found velocity.toml file"
  grep -q "antiBot.*enabled.*true" velocity.toml
  if [ $? -eq 0 ]; then
    echo "✓ AntiBot is enabled in velocity.toml"
  else
    echo "✗ AntiBot might not be enabled in velocity.toml. Please add or update:"
    echo "  [antiBot]"
    echo "    enabled = true"
  fi
else
  echo "✗ velocity.toml not found in current directory. Make sure you run this from your server root."
fi

# Check if antibot config exists
if [ -d "plugins/apibot" ] || [ -d "antibot" ]; then
  echo "✓ Found AntiBot configuration directory"
  
  if [ -f "plugins/apibot/config.yml" ]; then
    CONFIG_FILE="plugins/apibot/config.yml"
  elif [ -f "antibot/config.yml" ]; then
    CONFIG_FILE="antibot/config.yml"
  fi
  
  if [ ! -z "$CONFIG_FILE" ]; then
    echo "✓ Found AntiBot config file: $CONFIG_FILE"
    grep -q "general:.*enabled:.*true" "$CONFIG_FILE" || grep -q "enabled:.*true" "$CONFIG_FILE"
    if [ $? -eq 0 ]; then
      echo "✓ AntiBot is enabled in $CONFIG_FILE"
    else
      echo "✗ AntiBot might not be enabled in $CONFIG_FILE"
    fi
  else
    echo "✗ AntiBot config.yml not found"
  fi
else
  echo "✗ AntiBot configuration directory not found"
fi

# Check server logs
if [ -d "logs" ]; then
  echo "✓ Found logs directory"
  
  # Try to find the latest log file
  LATEST_LOG=$(find logs -name "*.log" -type f -printf '%T+ %p\n' | sort -r | head -n 1 | awk '{print $2}')
  
  if [ ! -z "$LATEST_LOG" ]; then
    echo "✓ Latest log file: $LATEST_LOG"
    
    # Check for ApiaryAntibot initialization
    grep -q "ApiaryAntibot" "$LATEST_LOG"
    if [ $? -eq 0 ]; then
      echo "✓ Found ApiaryAntibot mentions in logs"
      
      # Check for specific initialization messages
      grep "ApiaryAntibot.*enabled" "$LATEST_LOG"
      grep "Loaded configuration" "$LATEST_LOG"
      grep "attack.*detected" "$LATEST_LOG"
    else
      echo "✗ No mentions of ApiaryAntibot in logs"
    fi
  else
    echo "✗ No log files found"
  fi
else
  echo "✗ Logs directory not found"
fi

echo ""
echo "Recommendations:"
echo "1. Make sure AntiBot is enabled in velocity.toml"
echo "2. Make sure AntiBot is enabled in its own config.yml"
echo "3. Try lowering attack detection thresholds in config.yml for testing:"
echo "   min-players-for-attack: 3  (default is 8)"
echo "   min-attack-threshold: 1  (default is 2)"
echo "4. Check server console for any error messages during startup"
echo "5. Test with a few rapid connections to trigger the attack detection" 