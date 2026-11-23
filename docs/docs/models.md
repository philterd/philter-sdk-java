# Models

`getModels()` lists the local PhEye models the deployment has in `PHEYE_MODELS_DIRECTORY`, ordered by name, and `getModel(name)` reads one. A policy uses a model by giving its name as a ph-eye filter's `phEyeConfiguration.modelPath`, and the model then runs inside Philter with no PhEye service. Use the list, for example, to offer only the models a deployment has when building a policy.

```java
import ai.philterd.philter.model.Model;

for (Model model : client.getModels()) {
    System.out.println(model.getName() + " (" + model.getSizeBytes() + " bytes)");
}

Model model = client.getModel("pii-en-xsmall");
```

Both require Philter 4.0.0 or later and the `policies:read` scope. The models are the deployment's, the same for every user, and the list is empty when it has none. A key without the scope is a `ClientException` with status 403, and a name with no model one with status 404. Philter 4.1.0 and later return the list as `localModels` and deprecate the `models` field 4.0.x used; `getModels()` reads either.

| Getter | Value |
|--------|-------|
| `getName()` | The name a policy gives as `modelPath`. |
| `getEncoder()` | The encoder the model was trained from, or `null` when its configuration does not say. |
| `getMaxLength()` | The most words of a text the model reads; later words are not examined. `null` when its configuration does not say. |
| `getMaxWidth()` | The most words one detected entity can span. `null` when its configuration does not say. |
| `getSizeBytes()` | The combined size, in bytes, of its `gliner_config.json`, `tokenizer.json`, and `model.onnx`. |
| `getSha256()` | The SHA-256 of its `model.onnx`, as lowercase hex. Philter 4.1.0 and later; `null` from 4.0.x. |

The name does not identify a model's weights: a model replaced with different weights keeps its name and gets a new `getSha256()`. Record the SHA-256 with evaluation results to know which weights produced them.

Philter does not return a model's location on disk. For how models are deployed and named, see Philter's [models documentation](https://github.com/philterd/philter/blob/main/docs/docs/api_and_sdks/api/models_api.md).
