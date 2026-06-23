1.  **Analyze the existing code and issue:**
    *   The `TemplateResolver` reads templates from the classpath on every invocation.
    *   This is inefficient because templates are static files.
    *   The task requires caching the resolved templates to improve performance.

2.  **Establish a Baseline:**
    *   I've already created a benchmark (`TemplateResolverBenchmark`) to measure the performance of the current implementation.
    *   The baseline average time per `resolve` was roughly `34,828 ns`.

3.  **Implement the Optimization:**
    *   Modify `TemplateResolver.java` to use a `ConcurrentHashMap<String, String>` to cache the content of the templates.
    *   The `resolve` method should first check the cache.
    *   If the template is in the cache, return it.
    *   If not, read the template from the classpath, store it in the cache, and return it.
    *   Use `computeIfAbsent` for thread-safe caching without explicit locking or duplicate reads in high concurrency scenarios.
    *   Ensure exceptions (like `TemplateNotFoundException`) are still thrown correctly when a template isn't found on the classpath. Wait, `computeIfAbsent` might wrap exceptions or be tricky if I throw checked exceptions, but since we throw `RuntimeException` derivatives (`TemplateNotFoundException` is a `RuntimeException`), it should work fine within `computeIfAbsent`, or I can handle the absence check carefully. Let's look at `TemplateNotFoundException`. Assuming it extends `RuntimeException`. I'll verify this.

4.  **Verify the Impact:**
    *   Run the benchmark again to measure the performance improvement.
    *   Run `mvn test` to ensure that all existing unit tests pass, and specifically my added `TemplateResolverTest` passes.
    *   Run the project's formatting and linting tools if available.

5.  **Pre-commit steps:**
    *   Complete pre-commit steps to make sure proper testing, verifications, reviews, and reflections are done.

6.  **Submit:**
    *   Create a Pull Request with the benchmark results and clear description of changes.
