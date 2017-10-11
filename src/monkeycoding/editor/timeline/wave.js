goog.provide("monkeycoding.editor.timeline.wave");


const MsToPxRatio = 10;
const SegmentMs   = 80;


class Wave {
    constructor(canvas, {onWidthChange}) {
      this._ctx = canvas.getContext('2d');
      this._canvas = canvas;
      this._bounds = canvas.getBoundingClientRect();


      this._stream = [];
      this._segments = [];

      this._onWidthChange = (onWidthChange || function() {});
      this._onSeek = () => null;

    }


    setStream(stream) {
      this._stream = stream || [];
      this._calculateSegments();
      this._adjustWidth();
    }


    render() {
      let ctx = this._ctx;
      let segments = this._segments;

      let h = this._bounds.height;
      let w = this._bounds.width;


      ctx.clearRect(0,0, w, h);

      ctx.fillStyle = "#006495";
      ctx.strokeStyle = "006495";

      for (let i = 0; i < segments.length; i++) {
        let segment = segments[i];
        let rectH = h * Math.min(0.6, (segment.length*1.2 / 10));

        if (segment.length)
          ctx.fillRect((i * 80) / MsToPxRatio, (h/2 - rectH/2), (SegmentMs-20) / MsToPxRatio, rectH);

          ctx.strokeRect((i * 80) / MsToPxRatio, (h/2 - rectH/2), (SegmentMs-20) / MsToPxRatio, rectH);
      }

    }

    _adjustWidth() {
      let parentBounds = this._canvas.parentElement.getBoundingClientRect();
      let width = ((this._segments.length+10) * SegmentMs) / MsToPxRatio;

      this._canvas.width = Math.max(parentBounds.width, width);

      // setting canvas size will the transform matrix
      // this hack should disable sub-pixel AA
      this._ctx.translate(0.5, 0.5);

      this._bounds = this._canvas.getBoundingClientRect();
      this._onWidthChange(this._canvas.width);
    }


    _calculateSegments() {
        let stream   = this._stream;
        let segments = [[]];

        for (let i = 0, t = 0; i < stream.length; i++) {
            t += stream[i].dt;

            while(segments.length * SegmentMs < t) // pad
              segments.push([]);

            segments[segments.length-1].push(stream[i]);
        }

        this._segments = (segments.length > 1 || segments[0].length) ? segments : [];
    }
}





monkeycoding.editor.timeline.wave.Wave = Wave;
