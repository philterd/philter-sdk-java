namespace Philter.Model
{
    public class Span
    { 

        private int _characterStart;
        private int _characterEnd;
        private string _context;
        private string _documentId;
        private double _confidence;
        private string _replacement;
        private FilterType _filterType;

        public int characterStart
        {
            get
            {
                return this._characterStart;
            }
            set
            {
                this._characterStart = value;
            }
        }

        public int characterEnd
        {
            get
            {
                return this._characterEnd;
            }
            set
            {
                this._characterEnd = value;
            }
        }

        public string context
        {
            get
            {
                return this._context;
            }
            set
            {
                this._context = value;
            }
        }

        public string documentId
        {
            get
            {
                return this._documentId;
            }
            set
            {
                this._documentId = value;
            }
        }

        public double confidence
        {
            get
            {
                return this._confidence;
            }
            set
            {
                this._confidence = value;
            }
        }

        public string replacement
        {
            get
            {
                return this._replacement;
            }
            set
            {
                this._replacement = value;
            }
        }

        public FilterType filterType
        {
            get
            {
                return this._filterType;
            }
            set
            {
                this._filterType = value;
            }
        }

    }
}
