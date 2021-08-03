
Pod::Spec.new do |s|
  s.name         = "RNImageview"
  s.version      = "1.0.0"
  s.summary      = "RNImageview"
  s.description  = <<-DESC
                  RNImageview
                   DESC
  s.homepage     = ""
  s.license      = "MIT"
  # s.license      = { :type => "MIT", :file => "FILE_LICENSE" }
  s.author             = { "author" => "author@domain.cn" }
  s.platform     = :ios, "7.0"
  s.source       = { :git => "https://github.com/author/RNImageview.git", :tag => "master" }
  s.source_files  = "RNImageview/**/*.{h,m}"
  s.requires_arc = true


  s.dependency "React"
  s.dependency "SDWebImage"

end

  