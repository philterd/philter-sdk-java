using System.Threading.Tasks;
using System.Net.Http;
using System.Net.Http.Headers;
using Philter.Model;
using Newtonsoft.Json;
using System.Collections.Generic;

namespace Philter
{
    public class PhilterClient
    {
        private readonly HttpClient client = new HttpClient();

        private string endpoint;

        public PhilterClient(string endpoint)
        {
          this.endpoint = endpoint;
        }

        public async Task<string> Filter(string text, string context)
        {

            //var parameters = new Dictionary<string, string> { { "c", context } };
            //var encodedContent = new FormUrlEncodedContent (parameters);

            client.DefaultRequestHeaders.Accept.Clear();
            client.DefaultRequestHeaders.Accept.Add(new MediaTypeWithQualityHeaderValue("text/plain"));

            var response = await client.PostAsync(endpoint + "/api/filter", new StringContent(text));
            var content = await response.Content.ReadAsStringAsync();
            
            return content;

        }

        public async Task<FilterResponse> Explain(string text, string context)
        {

            //var parameters = new Dictionary<string, string> { { "c", context } };
            //var encodedContent = new FormUrlEncodedContent(parameters);

            client.DefaultRequestHeaders.Accept.Clear();
            client.DefaultRequestHeaders.Accept.Add(new MediaTypeWithQualityHeaderValue("text/plain"));

            var response = await client.PostAsync(endpoint + "/api/explain", new StringContent(text));
            var content = await response.Content.ReadAsStringAsync();

            return JsonConvert.DeserializeObject<FilterResponse>(content);

        }

        public async Task<Status> GetStatus()
        {

            var response = await client.GetAsync(endpoint + "/api/status");
            var json = await response.Content.ReadAsStringAsync();

            var status = JsonConvert.DeserializeObject<Status>(json);

            return status;

        }

    }
}
