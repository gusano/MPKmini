MPKminiResponder {
  classvar <>debug = true;

  var <parent,
      <id,
      <name,
      <synth,
      <offset,
      <>settings,
      <controls,
      <currentVol,
      <currentValues,
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

    settings = parent.settings;
    controls = this.getControls();
    //controls = parent.dict[name].argNames.copyRange(0, 7);
    currentValues = ();

    this.initToggleFunc();
    this.initLearnsFunc();
    this.initVolumeFunc();
    this.initControlsFunc();
  }

  initToggleFunc {
    toggleFunc = MIDIFunc.cc({ |val|

      if (val > 0 and: { synth.isNil }, {
        synth = Synth(name, currentValues.getPairs());

        this.debug("%> play".format(name))
      }, {
        try {
          synth.free();
          synth = nil;
          this.debug("%> stop".format(name))
        };
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
      var newVol, oldVol;
      if (parent.hasLearnActive().not, {
        oldVol = currentValues[\vol];
        newVol = val / 127;
        [\oldVol, oldVol, \newVol, newVol].postcs;
        // try to soft set volume
        if (oldVol.notNil, {
          if ((oldVol - newVol).abs <= 0.1, {
            synth.set(\vol, newVol);
            currentValues.add(\vol -> newVol);
            this.debug("%> vol %".format(name, newVol.round(0.01)))
          })
        }, {
          synth.set(\vol, newVol);
          currentValues.add(\vol -> newVol);
          this.debug("%> vol %".format(name, newVol.round(0.01)))
        });
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

        synth.set(control, newValue);
        currentValues.add(control -> newValue);

        this.debug("%> % %".format(name, control, newValue.round(0.01)));
      })
    }, ids)
  }

  recallValues {
    currentValues.keysValuesDo { |ctrl, val| parent.proxyspace[name].set(ctrl, val) }
  }

  getControls {
    var skip, ctls;

    ctls = List();
    skip = [\out, \vol];

    SynthDescLib.global.at(name).controls.do { |ctl|
      (skip.indexOf(ctl.name).isNil).if { ctls.add(ctl.name) }
    }

    ^ctls
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
    this.class.debug.if { args.postcs }
  }
}