require "json"

package = JSON.parse(File.read(File.join(__dir__, "package.json")))

Pod::Spec.new do |s|
  s.name         = "react-native-motion-sensors"
  s.version      = package["version"]
  s.summary      = package["description"]
  s.homepage     = package["repository"]["url"]
  s.license      = package["license"]
  s.author       = "Serpath"
  s.platforms    = { :ios => "15.0" }
  s.source       = { :git => package["repository"]["url"], :tag => s.version }
  s.source_files = "ios/**/*.{h,m,mm,swift}"
  # Keep the TurboModule spec out of the Swift module's umbrella header.
  # Swift's Clang Importer parses the umbrella in Objective-C mode and cannot
  # resolve the C++ stdlib headers (<utility>, <optional>, <tuple>) that the
  # codegen spec transitively pulls in.
  s.private_header_files = "ios/MotionSensorsModule.h"
  s.swift_version = "5.0"

  install_modules_dependencies(s)
end
