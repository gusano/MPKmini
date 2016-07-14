MPKmini {
  classvar <>debug = true;

  var <dict,
      <responders,
      <>globalLearnMode,
      <settings,
      <settingsFile,
      <mandatorySettings;

  *new { |dict, settingsFile|
    ^super.newCopyArgs(dict, settingsFile).init();
  }

  init {
    MIDIIn.connectAll();

    mandatorySettings = ["pads1", "pads2", "knobs", "notes"];
    globalLearnMode = Array.newClear(8);
    responders = ();
    settings = ();
    settingsFile = settingsFile ? 'settings.yaml';
    this.loadSettingsFile(settingsFile);
    this.addSynthDefs();
  }

  // All volume knobs are desactivated if at least one proxy is in learn mode
  hasLearnActive {
    ^globalLearnMode.indexOf(true).notNil
  }

  free {
    responders.keys.do(_.free())
  }

  map { |name, index, offset = 0|
    var resp = MPKminiResponder.new(index, name, offset, this);

    if (responders.includesKey(name), {
      responders[name].free()
    });
    responders.add(name -> resp);
  }

  addSynthDefs {
    dict.keysValuesDo { |name, func|
      this.wrapSynthDef(name, func)
    }
  }

  wrapSynthDef { |name, func|
    var sDef = SynthDef(name, { |out=0, vol=0|
      var snd, volMap;

      volMap = { |volume|
        if (volume == 0, {
          0
        }, {
          ((vol).log2 * 12).dbamp
        })
      };
      snd = SynthDef.wrap(func);
      Out.ar(out, snd * volMap.(vol));
    }).add;
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