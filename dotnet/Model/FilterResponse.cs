namespace Philter.Model
{
    public class FilterResponse
    { 

        private string _filteredText;
        private string _context;
        private string _documentId;
        private Explanation _explanation;

        public string filteredText
        {
            get
            {
                return this._filteredText;
            }
            set
            {
                this._filteredText = value;
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

        public Explanation explanation
        {
            get
            {
                return this._explanation;
            }
            set
            {
                this._explanation = value;
            }
        }

    }
}
