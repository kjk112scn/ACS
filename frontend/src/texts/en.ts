import type { TextsType } from './ko'

export const en: TextsType = {
  // Common
  common: {
    save: 'Save',
    cancel: 'Cancel',
    confirm: 'Confirm',
    delete: 'Delete',
    edit: 'Edit',
    add: 'Add',
    search: 'Search',
    reset: 'Reset',
    close: 'Close',
    back: 'Back',
    next: 'Next',
    previous: 'Previous',
    loading: 'Loading...',
    success: 'Success',
    error: 'Error',
    warning: 'Warning',
    info: 'Information',
    yes: 'Yes',
    no: 'No',
  },

  // Login
  login: {
    title: 'Login',
    username: 'Username',
    password: 'Password',
    loginButton: 'Login',
    invalidCredentials: 'Invalid credentials.',
    loginSuccess: 'Login successful.',
  },

  // Dashboard
  dashboard: {
    title: 'Dashboard',
    serverStatus: 'Server Status',
    connected: 'Connected',
    connecting: 'Connecting...',
    disconnected: 'Disconnected',
    communication: 'Communication',
    commandTime: 'Command Time',
    azimuth: 'Azimuth',
    elevation: 'Elevation',
    train: 'Train',
    cmd: 'CMD',
    actual: 'Actual',
    speed: 'Speed',
  },

  // Modes
  modes: {
    standby: 'Standby',
    step: 'Step',
    slew: 'Slew',
    pedestal: 'Pedestal',
    ephemeris: 'Ephemeris',
    passSchedule: 'Pass Schedule',
    sunTrack: 'Sun Track',
    feed: 'Feed',
  },

  // Settings
  settings: {
    title: 'Settings',
    theme: 'Theme',
    darkMode: 'Dark Mode',
    lightMode: 'Light Mode',

    // Language settings
    language: {
      current: 'Current Language',
      changed: (language: string) => `Changed to ${language}`,
      select: 'Select a language',
    },

    // Version info
    version: {
      title: 'Firmware Version Information',
      refresh: 'Refresh Version Information',
      loading: 'Loading version information...',
      error: 'An error occurred while loading version information',
      retry: 'Retry',
      noData: 'Click the refresh button to load version information',
      success: 'Version information loaded successfully',
      firmware: 'Firmware Version',
      serial: 'Serial Number',
      boards: {
        mainboard: 'Mainboard',
        mainboardDesc: 'Mainboard Firmware and Serial Number',
        azimuth: 'Azimuth',
        azimuthDesc: 'Azimuth Axis Firmware and Serial Number',
        elevation: 'Elevation',
        elevationDesc: 'Elevation Axis Firmware and Serial Number',
        tilt: 'Tilt',
        tiltDesc: 'Tilt Axis Firmware and Serial Number',
        feed: 'Feed',
        feedDesc: 'Feed Firmware and Serial Number',
      },
    },

    // Admin settings
    admin: {
      title: 'Admin Settings',
      description: 'System administrator settings',
      servoPreset: {
        title: 'Servo Preset',
        description: 'Servo encoder preset settings',
        azimuthDesc: 'Azimuth Axis Servo Encoder Preset',
        elevationDesc: 'Elevation Axis Servo Encoder Preset',
        tiltDesc: 'Tilt Axis Servo Encoder Preset',
        azimuthButton: 'Azimuth Preset',
        elevationButton: 'Elevation Preset',
        tiltButton: 'Tilt Preset',
        confirmTitle: 'Servo Preset Confirmation',
        confirmMessage: (axis: string) => `Execute ${axis} axis servo preset?`,
      },
      servoAlarmReset: 'Servo Alarm Reset',
      mcOnOff: 'M/C On/Off',
      confirm: 'Are you sure you want to execute?',
      success: 'Command executed successfully',
      error: 'An error occurred while executing command',
      axes: {
        azimuth: 'Azimuth',
        elevation: 'Elevation',
        tilt: 'Tilt',
      },
      states: {
        on: 'ON',
        off: 'OFF',
      },

      // Servo preset details
      servoPresetDetails: {
        azimuthDesc: 'Azimuth Axis Servo Encoder Preset',
        elevationDesc: 'Elevation Axis Servo Encoder Preset',
        tiltDesc: 'Tilt Axis Servo Encoder Preset',
        azimuthButton: 'Azimuth Preset',
        elevationButton: 'Elevation Preset',
        tiltButton: 'Tilt Preset',
        confirmTitle: 'Servo Preset Confirmation',
        confirmMessage: (axis: string) => `Execute ${axis} axis servo preset?`,
      },

      // Servo alarm reset details
      servoAlarmResetDetails: {
        azimuthDesc: 'Azimuth Axis Servo Alarm Reset',
        elevationDesc: 'Elevation Axis Servo Alarm Reset',
        tiltDesc: 'Tilt Axis Servo Alarm Reset',
        azimuthButton: 'Azimuth Reset',
        elevationButton: 'Elevation Reset',
        tiltButton: 'Tilt Reset',
        confirmTitle: 'Servo Alarm Reset Confirmation',
        confirmMessage: (axis: string) => `Reset ${axis} axis servo alarm?`,
      },

      // M/C On/Off details
      mcOnOffDetails: {
        title: 'M/C Status Control',
        description: 'Execute M/C On/Off command',
        confirmTitle: 'M/C On/Off Confirmation',
        confirmMessage: (state: string) => `Execute M/C ${state} command?`,
      },
    },

    // General settings
    general: {
      title: 'General Settings',
      theme: 'Theme Settings',
      language: 'Language Settings',
    },

    // Connection settings
    connection: {
      title: 'Connection Settings',
      websocket: 'WebSocket Server Address',
      api: 'API Base URL',
      autoReconnect: 'Auto-reconnect when disconnected',
    },

    // Tab titles
    generalTab: 'General Settings',
    systemTab: 'System Settings',
    connectionTab: 'Connection Settings',
    languageTab: 'Language Settings',
    versionTab: 'Version Information',
    adminTab: 'Admin Settings',
  },

  // Common buttons
  buttons: {
    save: 'Save',
    cancel: 'Cancel',
    confirm: 'Confirm',
    yes: 'Yes',
    no: 'No',
    refresh: 'Refresh',
    retry: 'Retry',
    close: 'Close',
  },

  // Error Messages
  errors: {
    apiError: 'An error occurred during API request.',
    networkError: 'Network connection problem.',
    systemError: 'System error occurred.',
    validationError: 'Invalid input value.',
    userError: 'User error occurred.',
    requiredField: 'This field is required.',
    invalidFormat: 'Invalid format.',
    minLength: (min: number) => `Please enter at least ${min} characters.`,
    maxLength: (max: number) => `Maximum ${max} characters allowed.`,
    minValue: (min: number) => `Minimum value is ${min}.`,
    maxValue: (max: number) => `Maximum value is ${max}.`,
    emailFormat: 'Invalid email format.',
    numericOnly: 'Only numbers are allowed.',
  },

  // Notifications
  notifications: {
    success: 'Success',
    error: 'Error',
    warning: 'Warning',
    info: 'Information',
    dismissAll: 'Dismiss all notifications.',
  },

  // Dialogs
  dialogs: {
    confirm: 'Confirm',
    alert: 'Alert',
    input: 'Input',
    inputLabel: 'Please enter a value',
    ok: 'OK',
    cancel: 'Cancel',
  },

  // ICD Related
  icd: {
    title: 'ICD Data',
    realTime: 'Real Time',
    status: 'Status',
    data: 'Data',
    connection: 'Connection',
    error: 'Error',
  },

  // Hardware Error Messages
  hardwareErrors: {
    // Servo Alarms
    ELEVATION_SERVO_ALARM: 'Elevation Servo Alarm',
    ELEVATION_SERVO_ALARM_CODE1: 'Elevation Servo Alarm Code 1',
    ELEVATION_SERVO_ALARM_CODE2: 'Elevation Servo Alarm Code 2',
    ELEVATION_SERVO_ALARM_CODE3: 'Elevation Servo Alarm Code 3',
    ELEVATION_SERVO_ALARM_CODE4: 'Elevation Servo Alarm Code 4',
    ELEVATION_SERVO_ALARM_CODE5: 'Elevation Servo Alarm Code 5',
    AZIMUTH_SERVO_ALARM: 'Azimuth Servo Alarm',
    AZIMUTH_SERVO_ALARM_CODE1: 'Azimuth Servo Alarm Code 1',
    AZIMUTH_SERVO_ALARM_CODE2: 'Azimuth Servo Alarm Code 2',
    AZIMUTH_SERVO_ALARM_CODE3: 'Azimuth Servo Alarm Code 3',
    AZIMUTH_SERVO_ALARM_CODE4: 'Azimuth Servo Alarm Code 4',
    AZIMUTH_SERVO_ALARM_CODE5: 'Azimuth Servo Alarm Code 5',
    TRAIN_SERVO_ALARM: 'Train Servo Alarm',
    TRAIN_SERVO_ALARM_CODE1: 'Train Servo Alarm Code 1',
    TRAIN_SERVO_ALARM_CODE2: 'Train Servo Alarm Code 2',
    TRAIN_SERVO_ALARM_CODE3: 'Train Servo Alarm Code 3',
    TRAIN_SERVO_ALARM_CODE4: 'Train Servo Alarm Code 4',
    TRAIN_SERVO_ALARM_CODE5: 'Train Servo Alarm Code 5',

    // Encoder Errors
    ELEVATION_ENCODER_ERROR: 'Elevation Encoder Error',
    AZIMUTH_ENCODER_ERROR: 'Azimuth Encoder Error',
    TRAIN_ENCODER_ERROR: 'Train Encoder Error',

    // Power Related
    POWER_SURGE_PROTECTOR_FAULT: 'Power Surge Protector Fault',
    POWER_REVERSE_PHASE_FAULT: 'Power Reverse Phase Fault',

    // Emergency Stop
    EMERGENCY_STOP_ACU: 'Emergency Stop ACU',
    EMERGENCY_STOP_POSITIONER: 'Emergency Stop Positioner',

    // Servo Brake
    ELEVATION_SERVO_BRAKE_ENGAGED: 'Elevation Servo Brake Engaged',
    AZIMUTH_SERVO_BRAKE_ENGAGED: 'Azimuth Servo Brake Engaged',
    TRAIN_SERVO_BRAKE_ENGAGED: 'Train Servo Brake Engaged',

    // Servo Motor
    ELEVATION_SERVO_MOTOR_ON: 'Elevation Servo Motor On',
    AZIMUTH_SERVO_MOTOR_ON: 'Azimuth Servo Motor On',
    TRAIN_SERVO_MOTOR_ON: 'Train Servo Motor On',

    // Limit Switches
    ELEVATION_LIMIT_SWITCH_POSITIVE_180: 'Elevation Limit Switch +180°',
    ELEVATION_LIMIT_SWITCH_POSITIVE_185: 'Elevation Limit Switch +185°',
    ELEVATION_LIMIT_SWITCH_NEGATIVE_0: 'Elevation Limit Switch -0°',
    ELEVATION_LIMIT_SWITCH_NEGATIVE_5: 'Elevation Limit Switch -5°',
    AZIMUTH_LIMIT_SWITCH_NEGATIVE_275: 'Azimuth Limit Switch -275°',
    AZIMUTH_LIMIT_SWITCH_POSITIVE_275: 'Azimuth Limit Switch +275°',
    TRAIN_LIMIT_SWITCH_NEGATIVE_275: 'Train Limit Switch -275°',
    TRAIN_LIMIT_SWITCH_POSITIVE_275: 'Train Limit Switch +275°',

    // Stow Pins
    ELEVATION_STOW_PIN_ACTIVE: 'Elevation Stow Pin Active',
    AZIMUTH_STOW_PIN_ACTIVE: 'Azimuth Stow Pin Active',
    TRAIN_STOW_PIN_ACTIVE: 'Train Stow Pin Active',

    // Servo Power
    SERVO_TRAIN_POWER_OFF: 'Train Servo Power Off',
    SERVO_ELEVATION_POWER_OFF: 'Elevation Servo Power Off',
    SERVO_AZIMUTH_POWER_OFF: 'Azimuth Servo Power Off',

    // Protocol Errors
    PROTOCOL_ELEVATION_ERROR: 'Elevation Protocol Error',
    PROTOCOL_AZIMUTH_ERROR: 'Azimuth Protocol Error',
    PROTOCOL_TRAIN_ERROR: 'Train Protocol Error',
    PROTOCOL_FEED_ERROR: 'Feed Protocol Error',

    // S-Band Errors
    S_BAND_LNA_LHCP_ERROR: 'S-Band LNA LHCP Error',
    S_BAND_LNA_RHCP_ERROR: 'S-Band LNA RHCP Error',
    S_BAND_RF_SWITCH_ERROR: 'S-Band RF Switch Error',

    // X-Band Errors
    X_BAND_LNA_LHCP_ERROR: 'X-Band LNA LHCP Error',
    X_BAND_LNA_RHCP_ERROR: 'X-Band LNA RHCP Error',

    // Fan Error
    FAN_ERROR: 'Fan Error',

    // Power On States
    S_BAND_LNA_LHCP_POWER_ON: 'S-Band LNA LHCP Power On',
    S_BAND_LNA_RHCP_POWER_ON: 'S-Band LNA RHCP Power On',
    S_BAND_RF_SWITCH_RHCP: 'S-Band RF Switch RHCP',
    X_BAND_LNA_LHCP_POWER_ON: 'X-Band LNA LHCP Power On',
    X_BAND_LNA_RHCP_POWER_ON: 'X-Band LNA RHCP Power On',
    FAN_POWER_ON: 'Fan Power On',

    // Resolved Messages
    ELEVATION_SERVO_ALARM_RESOLVED: 'Elevation Servo Alarm Resolved',
    ELEVATION_SERVO_ALARM_CODE1_RESOLVED: 'Elevation Servo Alarm Code 1 Resolved',
    ELEVATION_SERVO_ALARM_CODE2_RESOLVED: 'Elevation Servo Alarm Code 2 Resolved',
    ELEVATION_SERVO_ALARM_CODE3_RESOLVED: 'Elevation Servo Alarm Code 3 Resolved',
    ELEVATION_SERVO_ALARM_CODE4_RESOLVED: 'Elevation Servo Alarm Code 4 Resolved',
    ELEVATION_SERVO_ALARM_CODE5_RESOLVED: 'Elevation Servo Alarm Code 5 Resolved',
    AZIMUTH_SERVO_ALARM_RESOLVED: 'Azimuth Servo Alarm Resolved',
    AZIMUTH_SERVO_ALARM_CODE1_RESOLVED: 'Azimuth Servo Alarm Code 1 Resolved',
    AZIMUTH_SERVO_ALARM_CODE2_RESOLVED: 'Azimuth Servo Alarm Code 2 Resolved',
    AZIMUTH_SERVO_ALARM_CODE3_RESOLVED: 'Azimuth Servo Alarm Code 3 Resolved',
    AZIMUTH_SERVO_ALARM_CODE4_RESOLVED: 'Azimuth Servo Alarm Code 4 Resolved',
    AZIMUTH_SERVO_ALARM_CODE5_RESOLVED: 'Azimuth Servo Alarm Code 5 Resolved',
    TRAIN_SERVO_ALARM_RESOLVED: 'Train Servo Alarm Resolved',
    TRAIN_SERVO_ALARM_CODE1_RESOLVED: 'Train Servo Alarm Code 1 Resolved',
    TRAIN_SERVO_ALARM_CODE2_RESOLVED: 'Train Servo Alarm Code 2 Resolved',
    TRAIN_SERVO_ALARM_CODE3_RESOLVED: 'Train Servo Alarm Code 3 Resolved',
    TRAIN_SERVO_ALARM_CODE4_RESOLVED: 'Train Servo Alarm Code 4 Resolved',
    TRAIN_SERVO_ALARM_CODE5_RESOLVED: 'Train Servo Alarm Code 5 Resolved',
    ELEVATION_ENCODER_ERROR_RESOLVED: 'Elevation Encoder Error Resolved',
    AZIMUTH_ENCODER_ERROR_RESOLVED: 'Azimuth Encoder Error Resolved',
    TRAIN_ENCODER_ERROR_RESOLVED: 'Train Encoder Error Resolved',
    POWER_SURGE_PROTECTOR_FAULT_RESOLVED: 'Power Surge Protector Normal',
    POWER_REVERSE_PHASE_FAULT_RESOLVED: 'Power Reverse Phase Normal',
    EMERGENCY_STOP_ACU_RESOLVED: 'Emergency Stop ACU Released',
    EMERGENCY_STOP_POSITIONER_RESOLVED: 'Emergency Stop Positioner Released',
    ELEVATION_SERVO_BRAKE_ENGAGED_RESOLVED: 'Elevation Servo Brake Disengaged',
    AZIMUTH_SERVO_BRAKE_ENGAGED_RESOLVED: 'Azimuth Servo Brake Disengaged',
    TRAIN_SERVO_BRAKE_ENGAGED_RESOLVED: 'Train Servo Brake Disengaged',
    ELEVATION_SERVO_MOTOR_ON_RESOLVED: 'Elevation Servo Motor Off',
    AZIMUTH_SERVO_MOTOR_ON_RESOLVED: 'Azimuth Servo Motor Off',
    TRAIN_SERVO_MOTOR_ON_RESOLVED: 'Train Servo Motor Off',
    ELEVATION_LIMIT_SWITCH_POSITIVE_180_RESOLVED: 'Elevation Limit Switch +180° Inactive',
    ELEVATION_LIMIT_SWITCH_POSITIVE_185_RESOLVED: 'Elevation Limit Switch +185° Inactive',
    ELEVATION_LIMIT_SWITCH_NEGATIVE_0_RESOLVED: 'Elevation Limit Switch -0° Inactive',
    ELEVATION_LIMIT_SWITCH_NEGATIVE_5_RESOLVED: 'Elevation Limit Switch -5° Inactive',
    AZIMUTH_LIMIT_SWITCH_NEGATIVE_275_RESOLVED: 'Azimuth Limit Switch -275° Inactive',
    AZIMUTH_LIMIT_SWITCH_POSITIVE_275_RESOLVED: 'Azimuth Limit Switch +275° Inactive',
    TRAIN_LIMIT_SWITCH_NEGATIVE_275_RESOLVED: 'Train Limit Switch -275° Inactive',
    TRAIN_LIMIT_SWITCH_POSITIVE_275_RESOLVED: 'Train Limit Switch +275° Inactive',
    ELEVATION_STOW_PIN_ACTIVE_RESOLVED: 'Elevation Stow Pin Inactive',
    AZIMUTH_STOW_PIN_ACTIVE_RESOLVED: 'Azimuth Stow Pin Inactive',
    TRAIN_STOW_PIN_ACTIVE_RESOLVED: 'Train Stow Pin Inactive',
    SERVO_TRAIN_POWER_OFF_RESOLVED: 'Train Servo Power On',
    SERVO_ELEVATION_POWER_OFF_RESOLVED: 'Elevation Servo Power On',
    SERVO_AZIMUTH_POWER_OFF_RESOLVED: 'Azimuth Servo Power On',
    PROTOCOL_ELEVATION_ERROR_RESOLVED: 'Elevation Protocol Normal',
    PROTOCOL_AZIMUTH_ERROR_RESOLVED: 'Azimuth Protocol Normal',
    PROTOCOL_TRAIN_ERROR_RESOLVED: 'Train Protocol Normal',
    PROTOCOL_FEED_ERROR_RESOLVED: 'Feed Protocol Normal',
    S_BAND_LNA_LHCP_ERROR_RESOLVED: 'S-Band LNA LHCP Normal',
    S_BAND_LNA_RHCP_ERROR_RESOLVED: 'S-Band LNA RHCP Normal',
    S_BAND_RF_SWITCH_ERROR_RESOLVED: 'S-Band RF Switch Normal',
    X_BAND_LNA_LHCP_ERROR_RESOLVED: 'X-Band LNA LHCP Normal',
    X_BAND_LNA_RHCP_ERROR_RESOLVED: 'X-Band LNA RHCP Normal',
    FAN_ERROR_RESOLVED: 'Fan Normal',
    S_BAND_LNA_LHCP_POWER_ON_RESOLVED: 'S-Band LNA LHCP Power Off',
    S_BAND_LNA_RHCP_POWER_ON_RESOLVED: 'S-Band LNA RHCP Power Off',
    S_BAND_RF_SWITCH_RHCP_RESOLVED: 'S-Band RF Switch LHCP',
    X_BAND_LNA_LHCP_POWER_ON_RESOLVED: 'X-Band LNA LHCP Power Off',
    X_BAND_LNA_RHCP_POWER_ON_RESOLVED: 'X-Band LNA RHCP Power Off',
    FAN_POWER_ON_RESOLVED: 'Fan Power Off',

    // Test Errors
    TEST_ERROR: 'Test Error Occurred',
    TEST_ERROR_RESOLVED: 'Test Error Resolved',
  },

  // Hardware Error Log Panel
  hardwareErrorLog: {
    title: 'Hardware Error Log',
    category: 'Category',
    severity: 'Severity',
    startDate: 'Start Date',
    endDate: 'End Date',
    resolutionStatus: 'Resolution Status',
    search: 'Search',
    reset: 'Reset',
    realtimeUpdate: 'Real-time Update',
    updating: 'Updating...',
    loadMoreLogs: 'Load More Logs',
    loadingMoreLogs: 'Loading more logs...',
    showingLogs: (current: number, total: number) => `Showing ${current} of ${total} logs`,
    scrollHint: 'or scroll to load more',
    allLogsLoaded: (total: number) => `All ${total} logs loaded`,
    noLogsToDisplay: 'No error logs to display.',
    resolved: 'Resolved',
    active: 'Active',
  },

  // Charts
  charts: {
    azimuth: 'Azimuth',
    elevation: 'Elevation',
    train: 'Train',
    time: 'Time',
    angle: 'Angle',
    speed: 'Speed',
  },

  // System Settings
  system: {
    title: 'System Settings',
    categories: {
      location: {
        name: 'Location Settings',
        description: 'Latitude, Longitude, Altitude Settings',
      },
      tracking: {
        name: 'Tracking Settings',
        description: 'Tracking Interval, Duration, Minimum Elevation',
      },
      stow: {
        name: 'Stow Settings',
        description: 'Stow Angle and Speed Settings',
      },
      antennaSpec: {
        name: 'Antenna Specification',
        description: 'True North Offset, Tilt Angle',
      },
      angleLimits: {
        name: 'Angle Limits',
        description: 'Azimuth, Elevation, Train Limits',
      },
      speedLimits: {
        name: 'Speed Limits',
        description: 'Azimuth, Elevation, Train Speed Limits',
      },
      offsetLimits: {
        name: 'Offset Limits',
        description: 'Angle Offset, Time Offset Limits',
      },
      algorithm: {
        name: 'Algorithm Settings',
        description: 'Geo Min Motion and Algorithm Parameters',
      },
      stepSize: {
        name: 'Step Size Limits',
        description: 'Antenna Movement Step Size Limits',
      },
    },

    // Common messages
    common: {
      changed: 'Changed',
      saved: 'Saved',
      loading: 'Loading...',
      save: 'Save',
      reset: 'Reset',
      cancel: 'Cancel',
      apply: 'Apply',
      success: 'Settings saved successfully',
      error: 'Error occurred while saving settings',
      validationError: 'Please check your input values',
      applyRecommended: 'Apply Recommended Values',
      getCurrentLocation: 'Get Current Location',
    },

    // Location settings
    location: {
      title: 'Location Settings',
      latitude: 'Latitude (degrees)',
      longitude: 'Longitude (degrees)',
      altitude: 'Altitude (meters)',
      latitudeHint: 'Latitude range: -90° to 90°',
      longitudeHint: 'Longitude range: -180° to 180°',
      altitudeHint: 'Altitude must be 0 meters or higher',
      latitudeRequired: 'Please enter latitude',
      longitudeRequired: 'Please enter longitude',
      altitudeRequired: 'Please enter altitude',
      latitudeRange: 'Latitude must be between -90° and 90°',
      longitudeRange: 'Longitude must be between -180° and 180°',
      altitudeMin: 'Altitude must be 0 meters or higher',
    },

    // Tracking settings
    tracking: {
      title: 'Tracking Settings',
      interval: 'Tracking Interval (seconds)',
      duration: 'Tracking Duration (minutes)',
      minElevation: 'Minimum Elevation (degrees)',
      intervalHint: 'Tracking interval must be 1 second or higher',
      durationHint: 'Tracking duration must be 1 minute or higher',
      minElevationHint: 'Minimum elevation must be between 0° and 90°',
      intervalRequired: 'Please enter tracking interval',
      durationRequired: 'Please enter tracking duration',
      minElevationRequired: 'Please enter minimum elevation',
      intervalMin: 'Tracking interval must be 1 second or higher',
      durationMin: 'Tracking duration must be 1 minute or higher',
      minElevationRange: 'Minimum elevation must be between 0° and 90°',
    },

    // Stow settings
    stow: {
      title: 'Stow Settings',
      azimuthAngle: 'Azimuth Stow Angle (degrees)',
      elevationAngle: 'Elevation Stow Angle (degrees)',
      trainAngle: 'Train Stow Angle (degrees)',
      azimuthSpeed: 'Azimuth Stow Speed (deg/sec)',
      elevationSpeed: 'Elevation Stow Speed (deg/sec)',
      trainSpeed: 'Train Stow Speed (deg/sec)',
      azimuthAngleHint: 'Azimuth Stow angle range: -180° to 180°',
      elevationAngleHint: 'Elevation Stow angle range: 0° to 90°',
      trainAngleHint: 'Train Stow angle range: -180° to 180°',
      speedHint: 'Speed must be 0.1 deg/sec or higher',
      azimuthAngleRequired: 'Please enter Azimuth Stow angle',
      elevationAngleRequired: 'Please enter Elevation Stow angle',
      trainAngleRequired: 'Please enter Train Stow angle',
      azimuthSpeedRequired: 'Please enter Azimuth Stow speed',
      elevationSpeedRequired: 'Please enter Elevation Stow speed',
      trainSpeedRequired: 'Please enter Train Stow speed',
      speedMin: 'Speed must be 0.1 deg/sec or higher',
    },

    // Antenna spec settings
    antennaSpec: {
      title: 'Antenna Specification Settings',
      trueNorthOffset: 'True North Offset (degrees)',
      tiltAngle: 'Tilt Angle (degrees)',
      trueNorthOffsetHint: 'True North Offset range: -180° to 180°',
      tiltAngleHint: 'Tilt Angle range: -90° to 90°',
      trueNorthOffsetRequired: 'Please enter True North Offset',
      tiltAngleRequired: 'Please enter Tilt Angle',
      trueNorthOffsetRange: 'True North Offset must be between -180° and 180°',
      tiltAngleRange: 'Tilt Angle must be between -90° and 90°',
    },

    // Angle limits settings
    angleLimits: {
      title: 'Angle Limits Settings',
      azimuthMin: 'Azimuth Min Angle (degrees)',
      azimuthMax: 'Azimuth Max Angle (degrees)',
      elevationMin: 'Elevation Min Angle (degrees)',
      elevationMax: 'Elevation Max Angle (degrees)',
      trainMin: 'Train Min Angle (degrees)',
      trainMax: 'Train Max Angle (degrees)',
      azimuthHint: 'Azimuth range: -180° to 180°',
      elevationHint: 'Elevation range: 0° to 90°',
      trainHint: 'Train range: -180° to 180°',
      azimuthMinRequired: 'Please enter Azimuth min angle',
      azimuthMaxRequired: 'Please enter Azimuth max angle',
      elevationMinRequired: 'Please enter Elevation min angle',
      elevationMaxRequired: 'Please enter Elevation max angle',
      trainMinRequired: 'Please enter Train min angle',
      trainMaxRequired: 'Please enter Train max angle',
      azimuthRange: 'Azimuth must be between -180° and 180°',
      elevationRange: 'Elevation must be between 0° and 90°',
      trainRange: 'Train must be between -180° and 180°',
    },

    // Speed limits settings
    speedLimits: {
      title: 'Speed Limits Settings',
      azimuthMax: 'Azimuth Max Speed (deg/sec)',
      elevationMax: 'Elevation Max Speed (deg/sec)',
      trainMax: 'Train Max Speed (deg/sec)',
      speedHint: 'Speed must be 0.1 deg/sec or higher',
      azimuthRequired: 'Please enter Azimuth max speed',
      elevationRequired: 'Please enter Elevation max speed',
      trainRequired: 'Please enter Train max speed',
      speedMin: 'Speed must be 0.1 deg/sec or higher',
    },

    // Offset limits settings
    offsetLimits: {
      title: 'Offset Limits Settings',
      angleOffset: 'Angle Offset (degrees)',
      timeOffset: 'Time Offset (seconds)',
      angleOffsetHint: 'Angle offset range: -180° to 180°',
      timeOffsetHint: 'Time offset range: -3600 to 3600 seconds',
      angleOffsetRequired: 'Please enter angle offset',
      timeOffsetRequired: 'Please enter time offset',
      angleOffsetRange: 'Angle offset must be between -180° and 180°',
      timeOffsetRange: 'Time offset must be between -3600 and 3600 seconds',
    },

    // Algorithm settings
    algorithm: {
      title: 'Algorithm Settings',
      geoMinMotion: 'Geo Min Motion (degrees)',
      trackingAccuracy: 'Tracking Accuracy (degrees)',
      predictionTime: 'Prediction Time (seconds)',
      geoMinMotionHint: 'Geo Min Motion range: 0.001° to 1°',
      trackingAccuracyHint: 'Tracking accuracy range: 0.001° to 1°',
      predictionTimeHint: 'Prediction time range: 1 to 3600 seconds',
      geoMinMotionRequired: 'Please enter Geo Min Motion',
      trackingAccuracyRequired: 'Please enter tracking accuracy',
      predictionTimeRequired: 'Please enter prediction time',
      geoMinMotionRange: 'Geo Min Motion must be between 0.001° and 1°',
      trackingAccuracyRange: 'Tracking accuracy must be between 0.001° and 1°',
      predictionTimeRange: 'Prediction time must be between 1 and 3600 seconds',
    },

    // Step size limits settings
    stepSize: {
      title: 'Step Size Limits Settings',
      minStepSize: 'Minimum Step Size (degrees)',
      maxStepSize: 'Maximum Step Size (degrees)',
      minStepSizeHint: 'Minimum step size limit',
      maxStepSizeHint: 'Maximum step size limit',
      minStepSizeRequired: 'Please enter minimum step size',
      maxStepSizeRequired: 'Please enter maximum step size',
      minStepSizeMin: 'Minimum step size must be 0.001° or higher',
      maxStepSizeMin: 'Maximum step size must be 0.001° or higher',
      minStepSizeMax: 'Minimum step size must be 180° or lower',
      maxStepSizeMax: 'Maximum step size must be 180° or lower',
      applyRecommended: 'Apply Recommended Values',
      descriptionTitle: 'Step Size Limits Description',
      description1: 'Step Size Limits:',
      description2:
        'Set the minimum/maximum angle that can be moved at once during antenna movement.',
      description3: 'These values are used for precise antenna control and safe movement.',
      minValueTitle: 'Minimum Value',
      minValueDesc: 'Prevents too small movements to ensure system stability',
      maxValueTitle: 'Maximum Value',
      maxValueDesc: 'Prevents too large movements to protect the antenna',
      recommendedRange: 'Recommended range: 0.1° to 180°',
    },
  },

  // Page-specific messages
  pages: {
    // Standby mode page
    standby: {
      standbyCommandSent: 'Standby command has been sent.',
      allStandbyCommandSent: 'All Standby command has been sent.',
      stowCommandSent: 'Stow command has been sent.',
      standbyCommand: 'Standby command',
      allStandbyCommand: 'All Standby command',
      stowCommand: 'Stow command',
    },
    // Sun Track mode page
    sunTrack: {
      speedDescription: 'Speed values are applied when the Go button is clicked.',
      startSuccess: 'Sun Track has been started.',
      startCommand: 'Sun Track start',
      stopSuccess: 'Sun Track has been stopped.',
      stopCommand: 'Sun Track stop',
      stowCommandSent: 'Stow command has been sent.',
      stowCommand: 'Stow command',
    },
    // Login page
    login: {
      invalidCredentials: 'Invalid credentials.',
    },
  },
} as const