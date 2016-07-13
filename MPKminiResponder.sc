MPKminiResponder {
  var <parent,
      <id,
      <name,
      //<proxy,
      <offset,
      <>settings,
      <controls,
      <currentVol,
      <currentValues,
      <audioFunc,
      <toggleFunc,
      <volumeFunc,
      <controlsFunc,
      <learnsFunc,
      <learnMode = false;

  *new { |aId, aName, aOffset, aParent|
    ^super.new.init(aId, aName, aOffset, aParent)
  }

  init { |aId, aName, aOffset, aParent|
    // I wish newCopyArgs would work here...
    id = aId;
    name = aName;
    offset = aOffset;
    parent = aParent;

    //proxy = parent.proxyspace.at(name);

    settings = parent.settings;
    // #1 - store controls –> store audio func as String
    controls = parent.funcDict[name].argNames.copyRange(0, 7);
    currentValues = ();

    audioFunc = parent.funcDict[name].asCompileString;
    this.initToggleFunc();
    this.initLearnsFunc();
    this.initVolumeFunc();
    this.initControlsFunc();
  }

  initToggleFunc {
    toggleFunc = MIDIFunc.cc({ |val|
      if (val > 0, {
        parent.proxyspace[name].source_(audioFunc.interpret);

        if (currentValues.size > 0, {
          this.recallValues();
        });
        if (currentVol.notNil, {
          parent.proxyspace[name].vol_(currentVol)
        });

        parent.proxyspace[name].play;

        this.debug("%> play".format(name))
      }, {
        if (parent.proxyspace[name].monitor.isPlaying, {
          parent.proxyspace[name].clear();
          this.debug("%> stop".format(name))
        })
      });
    }, settings["pads1"][id - 1])
  }

  initLearnsFunc {
    learnsFunc = MIDIFunc.cc({ |val|
      learnMode = (val > 0);
      parent.globalLearnMode.put(id - 1, val > 0);
      this.debug("%> learn %".format(name, learnMode));
    }, settings["pads2"][id - 1])
  }

  initVolumeFunc {
    volumeFunc = MIDIFunc.cc({ |val|
      if (parent.hasLearnActive().not, {
        parent.proxyspace[name].vol_(val / 127);
        currentVol = val / 127;
        this.debug("%> vol %".format(name, val.round(0.01)))
      });
    }, settings["knobs"][id - 1])
  }

  initControlsFunc {
    var ids = this.getIdsForControls();

    controlsFunc = MIDIFunc.cc( {|val, cc|
      var control, spec, controlIndex, newValue;

      if (learnMode, {
        controlIndex = settings["knobs"].indexOf(cc);
        if (offset > 0, { controlIndex = controlIndex + 1 - offset });
        control = controls[controlIndex];
        spec = control.asSpec ? [0, 127].asSpec;
        newValue = spec.map(val / 127);

        parent.proxyspace[name].set(control, newValue);
        currentValues.add(control -> newValue);

        this.debug("%> % %".format(name, control, newValue.round(0.01)));
      })
    }, ids)
  }

  recallValues {
    currentValues.keysValuesDo { |ctrl, val| parent.proxyspace[name].set(ctrl, val) }
  }

  getIdsForControls {
    this.checkUnmappedControls();
    ^settings["knobs"].copyRange(0 + (offset - 1), controls.size + (offset - 1));
  }

  checkUnmappedControls {
    var unmapped, rangeMax, size = controls.size;

    rangeMax = size + (offset - 1);
    if (rangeMax > 8) {
      unmapped = controls.reverse.copyRange(0, rangeMax - 9).reverse;
      ("The following controls won't be mapped:\n" + unmapped.asString).warn;
    }
  }

  free {
    toggleFunc.free();
    learnsFunc.free();
    volumeFunc.free();
    controlsFunc.free();
  }

  // @private
  debug { |...args|
    args.postcs;
  }
}