# Project name: Practical_devops_HW1

**Overview:**
This project is a simple example of a Python application called "Hello World". It includes a basic Python script, unit tests, and a Continuous Integration (CI) pipeline to automate testing using GitHub Actions.

**Local Installation:**
1. Clone this repository to your local machine.
2. Navigate to the project directory.
3. Make sure you have Python (version 3.7) installed.
4. Run the application:
   ```bash
   python hello_world.py
   ```

**CI Pipeline:**
The CI pipeline is triggered on both pull requests and pushes to the `master` branch. It consists of the following steps:
1. **Linting:** Checks the code for adherence to coding standards using pylint.
2. **Unit Testing:** Executes unit tests for the application.
3. **Build RPM Package:** Builds an RPM package using the `hello_world.spec` and `hello_world.py` files.
4. **Upload Artifact:** Uploads the RPM package as an artifact to be accessed later.

**Tools and Technologies:**
- Python: The programming language used for the application.
- pytest: A testing framework for writing and executing unit tests.
- pylint: A tool for checking code quality and adherence to coding standards.
- RPM: The packaging format used for building software packages.
- GitHub Actions: Used for continuous integration and automating the testing process.

**Repository Structure:**
- `.github/workflows/main_pipeline.yml`: The CI pipeline configuration file.
- `hello_world.py`: The main Python script containing the "Hello World" application.
- `hello_world.spec`: The specification file for building the RPM package.
- `test_hello_world.py`: Contains unit tests for the `hello_world.py` script.

**Running the CI Pipeline:**
The CI pipeline is automatically triggered on pull requests and pushes to the `master` branch. It includes linting, unit testing, and RPM package building. If all checks pass, the RPM package is uploaded as an artifact.

**Accessing the RPM Artifact:**
After a successful pipeline run, you can access the built RPM package in the "Artifacts" section of the GitHub Actions tab.

**Contributing:**
Contributions to this project are welcome! If you find any issues or improvements, feel free to open an issue or submit a pull request.

**License:**
This project is licensed under the [MIT License](LICENSE).
```
