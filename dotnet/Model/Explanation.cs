using System.Collections.Generic;

namespace Philter.Model
{
    public class Explanation
    {

        private List<Span> _appliedSpans;
        private List<Span> _identifiedSpans;

        public List<Span> appliedSpans
        {
            get
            {
                return this._appliedSpans;
            }
            set
            {
                this._appliedSpans = value;
            }
        }

        public List<Span> identifiedSpans
        {
            get
            {
                return this._identifiedSpans;
            }
            set
            {
                this._identifiedSpans = value;
            }
        }

    }
}
