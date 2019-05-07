using System;
using System.Collections.Generic;
using System.Net.Http;
using System.Net.Http.Headers;
using System.Runtime.Serialization.Json;
using System.Threading.Tasks;

namespace Philter
{
    class Client
    {
        private readonly HttpClient client = new HttpClient();

        private string endpoint;

        public Client(string endpoint)
        {
          this.endpoint = endpoint;
        }

        private async Task<string> filter(string text, string context)
        {

            //var parameters = new Dictionary<string, string> { { "c", context } };
            //var encodedContent = new FormUrlEncodedContent (parameters);

            client.DefaultRequestHeaders.Accept.Clear();
            client.DefaultRequestHeaders.Accept.Add(new MediaTypeWithQualityHeaderValue("text/plain"));

            var response = await client.PostAsync(endpoint + "/api/filter", new StringContent(text));
            var content = await response.Content.ReadAsStringAsync();
            
            return content;

        }
    }
}
