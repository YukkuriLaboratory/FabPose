{
  description = "A basic flake with a shell";
  inputs.nixpkgs.url = "github:NixOS/nixpkgs/nixpkgs-unstable";
  inputs.systems.url = "github:nix-systems/default";
  inputs.flake-utils = {
    url = "github:numtide/flake-utils";
    inputs.systems.follows = "systems";
  };

  outputs =
    { nixpkgs, flake-utils, ... }:
    flake-utils.lib.eachDefaultSystem (
      system:
      let
        pkgs = nixpkgs.legacyPackages.${system};
        libraries =
          with pkgs;
          lib.makeLibraryPath [
            libpulseaudio
            libGL
            udev
            flite
            libx11
            libxcursor
            libxi
            libxext
            libxxf86vm
            libxrandr
          ];
      in
      {
        formatter = pkgs.nixfmt-tree;
        devShells.default = pkgs.mkShell {
          packages = with pkgs; [
            bashInteractive
            xrandr
            xorg-server # Provides Xvfb for headless client tests
            xvfb-run # Fallback wrapper for running Gradle tasks outside the buildscript-managed Xvfb path
            gcc # Required by build.fabric.unobfuscated.gradle.kts to compile the PulseAudio stub
            pinact
            zizmor
          ];
          shellHook = ''
            export LD_LIBRARY_PATH=''${LD_LIBRARY_PATH:+$LD_LIBRARY_PATH:}${libraries}
          '';
        };
      }
    );
}
