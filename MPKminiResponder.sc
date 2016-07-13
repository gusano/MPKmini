MPKminiResponder {
  var <parent,
      <id,
      <name,
      <proxy,
      <offset,
      <>settings,
      <controls,
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

    proxy = parent.proxyspace.at(name);
    settings = parent.settings;
    controls = proxy.controlKeys.copyRange(0, 7);
    this.initToggleFunc();
    this.initLearnsFunc();
    this.initVolumeFunc();
    this.initControlsFunc();
  }

  initToggleFunc {
    toggleFunc = MIDIFunc.cc({ |val|
      if (val > 0, {
        proxy.play;
        this.debug("%> play".format(name))
      }, {
        proxy.stop;
        this.debug("%> stop".format(name))
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
        proxy.vol_(val / 127);
        this.debug("%> vol %".format(name, val.round(0.01)))
      });
    }, settings["knobs"][id - 1])
  }

  initControlsFunc {
    var ids = this.getIdsForControls();

    controlsFunc = MIDIFunc.cc( {|val, cc|
      var control, spec, controlIndex;

      if (learnMode, {
        controlIndex = settings["knobs"].indexOf(cc);
        if (offset > 0, { controlIndex = controlIndex + 1 - offset });
        control = controls[controlIndex];
        spec = control.asSpec ? [0, 127].asSpec;
        proxy.set(control, spec.map(val / 127));
        this.debug("%> % %".format(name, control, spec.map(val / 127).round(0.01)));
      })
    }, ids)
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