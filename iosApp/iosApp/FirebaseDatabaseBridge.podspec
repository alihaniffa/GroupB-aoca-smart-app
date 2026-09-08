Pod::Spec.new do |spec|
  spec.name         = 'FirebaseDatabaseBridge'
  spec.version      = '1.0.0'
  spec.summary      = 'Objective-C bridge for Firebase Realtime Database'
  spec.homepage     = 'https://example.com'
  spec.license      = { :type => 'MIT' }
  spec.author       = { 'Group B' => 'groupb@example.com' }

  spec.platform     = :ios, '18.0'

  spec.source       = {
    :path => '.'
  }

  spec.source_files = 'FirebaseDatabaseBridge.{h,m}'

  spec.public_header_files = 'FirebaseDatabaseBridge.h'

  spec.dependency 'FirebaseDatabase'
end