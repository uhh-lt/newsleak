#!/bin/sh

DIR=$(dirname "$0")

cd $DIR/..

if [[ $(git status -s) ]]
then
    echo "The working directory is dirty. Please commit any pending changes."
    exit 1;
fi

echo "Deleting old publication"
rm -rf newsleak.io
mkdir newsleak.io
git worktree prune
rm -rf .git/worktrees/newsleak.io/

echo "Checking out gh-pages branch into newsleak.io"
git worktree add -B gh-pages newsleak.io gh-pages

echo "Removing existing files"
rm -rf newsleak.io/*

echo "Generating site"
cd documentation
hugo
cd ..

echo "Updating gh-pages branch"
cd newsleak.io && git add --all && git commit -m "Publishing to gh-pages"
