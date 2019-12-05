namespace Philter.Model
{
    public class FilterType
    { 

        private string _type;
        private string _deterministic;

        public string type
        {
            get
            {
                return this._type;
            }
            set
            {
                this._type = value;
            }
        }

        public string deterministic
        {
            get
            {
                return this._deterministic;
            }
            set
            {
                this._deterministic = value;
            }
        }


    }
}
