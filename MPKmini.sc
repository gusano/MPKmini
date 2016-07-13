MPKmini {
  classvar <>debug = true;

  var <proxyspace,
      <funcDict,
      <responders,
      <>globalLearnMode,
      <settings,
      <settingsFile,
      <mandatorySettings;

  *new { |proxyspace, funcDict, settingsFile|
    ^super.newCopyArgs(proxyspace, funcDict, settingsFile).init();
  }

  init {
    MIDIIn.connectAll();

    mandatorySettings = ["pads1", "pads2", "knobs", "notes"];
    globalLearnMode = Array.newClear(8);
    responders = ();
    settings = ();
    settingsFile = settingsFile? 'settings.yaml';
    this.loadSettingsFile(settingsFile);
  }

  // All volume knobs are desactivated if at least one proxy is in learn mode
  hasLearnActive {
    ^globalLearnMode.indexOf(true).notNil
  }

  free {
    responders.keys.do(_.free())
  }

  map { |node, index, offset|
    var resp, name;

    name = this.prFixNodeName(node);
    offset = offset ? 0;

    resp = MPKminiResponder.new(index, name, offset, this);
    if (responders.includesKey(name), {
      responders[name].free()
    });
    responders.add(name -> resp);
  }

  loadSettingsFile { |file|
    var currentPath, filePath;

    currentPath = this.class.filenameSymbol.asString.dirname;
    filePath = currentPath +/+ file;

    File.exists(filePath).not.if {
      ("Settings file" + filePath + "is missing").throw
    };

    settings = (filePath).parseYAMLFile();
    this.prFixSettings();
    responders.keys.do { |key|
      responders[key].settings = settings;
    };
    this.debug("Settings used:" + filePath)
  }

  // @private
  prFixSettings {
    mandatorySettings.do { |key|
      settings.includesKey(key).not.if {
        "Missing key in settings:" + key
      }.throw
    };

    settings.keys.do { |key|
      settings[key] = settings[key].collect(_.asInteger)
    }
  }

  // @private
  prFixNodeName { |node|
    if (node.class === NodeProxy, {
      node = node.cs.asString.replace("~", "").asSymbol;
    });

    ^node;
  }

  // @private
  debug { |...args|
    this.class.debug.if { args.postln; ""; }
  }
}