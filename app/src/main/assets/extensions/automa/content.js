// content.js

const port = browser.runtime.connectNative("lundusbrowser");

function sleep(ms) {
    return new Promise(resolve => setTimeout(resolve, ms));
}

async function waitForElement(selector, timeout = 10000) {
    const start = Date.now();

    while (Date.now() - start < timeout) {
        const element = document.querySelector(selector);

        if (element) {
            return element;
        }

        await sleep(250);
    }

    throw new Error(`Element not found: ${selector}`);
}

async function executeClick(step) {
    const element = await waitForElement(
        step.target,
        step.timeout || 10000
    );

    element.scrollIntoView({
        behavior: "smooth",
        block: "center"
    });

    element.click();
}

async function executeInsert(step) {
    const element = await waitForElement(
        step.target,
        step.timeout || 10000
    );

    const value = step.value ?? "";

    element.focus();

    // React/Vue/modern frameworks
    const prototype = Object.getPrototypeOf(element);
    const descriptor = Object.getOwnPropertyDescriptor(
        prototype,
        "value"
    );

    if (descriptor && descriptor.set) {
        descriptor.set.call(element, value);
    } else {
        element.value = value;
    }

    element.dispatchEvent(
        new Event("input", {
            bubbles: true
        })
    );

    element.dispatchEvent(
        new Event("change", {
            bubbles: true
        })
    );
}

async function executeJavascript(step) {
    const code = step.value || "";

    const result = Function(`
        return (async () => {
            ${code}
        })();
    `)();

    await result;
}

async function executeDelay(step) {
    await sleep(Number(step.value || 0));
}

async function executeStep(step) {
    if (!step || !step.action) {
        throw new Error("Invalid workflow step");
    }

    switch (step.action.toLowerCase()) {

        case "click":
            await executeClick(step);
            break;

        case "insert":
            await executeInsert(step);
            break;

        case "execute":
            await executeJavascript(step);
            break;

        case "delay":
            await executeDelay(step);
            break;

        default:
            throw new Error(
                `Unknown action: ${step.action}`
            );
    }
}

async function runWorkflow(steps) {
    if (!Array.isArray(steps)) {
        throw new Error("Workflow must be an array");
    }

    for (let i = 0; i < steps.length; i++) {
        const step = steps[i];

        console.log(
            `[Workflow] Step ${i + 1}/${steps.length}`,
            JSON.stringify(step)
        );

        await executeStep(step);
    }

    return true;
}

port.onMessage.addListener(async message => {

    if (!message) {
        return;
    }

    if (message.type !== "runWorkflow") {
        return;
    }

    try {

        await runWorkflow(message.steps);

        port.postMessage({
            type: "workflowResult",
            success: true
        });

    } catch (error) {

        port.postMessage({
            type: "workflowResult",
            success: false,
            error: error?.message || String(error)
        });
    }
});

port.postMessage({
    type: "ready",
    url: location.href
});