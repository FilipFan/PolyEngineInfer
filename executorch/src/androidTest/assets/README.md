The test model was prepared using the [Model preparation](https://github.com/pytorch/executorch/tree/v0.7.0-rc1/examples/demo-apps/android/LlamaDemo#model-preparation), with parameters based on the [llama2.c models](https://github.com/karpathy/llama2.c/tree/master?tab=readme-ov-file#models).

```
curl -C - -Ls "https://huggingface.co/karpathy/tinyllamas/resolve/main/stories15M.pt" --output stories15M.pt
curl -C - -Ls "https://raw.githubusercontent.com/karpathy/llama2.c/master/tokenizer.model" --output tokenizer.model
touch params.json
echo '{"dim": 288, "multiple_of": 32, "n_heads": 6, "n_layers": 6, "norm_eps": 1e-05, "vocab_size": 32000}' > params.json
python -m extension.llm.export.export_llm base.checkpoint=stories15M.pt base.params=params.json model.dtype_override="fp16" export.output_name=stories15M_h.pte model.use_kv_cache=True
python -m pytorch_tokenizers.tools.llama2c.convert -t tokenizer.model -o tokenizer.bin
```
