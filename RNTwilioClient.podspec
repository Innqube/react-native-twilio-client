require 'json'

spec = JSON.load(File.read(File.expand_path("./package.json", __dir__)))

Pod::Spec.new do |s|
  s.name         = "RNTwilioClient"
  s.version      = spec['version']
  s.summary      = spec['description']
  s.authors      = spec['author']['name']
  s.homepage     = spec['homepage']
  s.license      = spec['license']
  s.platform     = :ios, "8.1"

  s.source_files = [ "ios/RNTwilioClient/*.h", "ios/RNTwilioClient/*.m"]
  s.source = {:path => "./RNTwilioClient"}

  s.dependency 'React'
  s.xcconfig = { 'FRAMEWORK_SEARCH_PATHS' => '${PODS_ROOT}/TwilioClient' }
  s.frameworks   = 'TwilioClient'
end