%global __python /usr/bin/python3

Name: hello_world
Version: 1.0.0
Release: 1%{?dist}
Summary: A simple "Hello, World!" package

License: MIT
URL: https://github.com/your_username/hello_world

BuildArch: noarch

Requires: python3

%description
This package contains a simple Python script that prints "Hello, World!"

%prep

%build

%install
rm -rf %{buildroot}
mkdir -p %{buildroot}%{_bindir}
cp %{SOURCE0} %{buildroot}%{_bindir}

%files
%{_bindir}/hello_world.py

%changelog
